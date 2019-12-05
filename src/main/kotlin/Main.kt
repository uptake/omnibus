package com.uptake.omnibus

import clients.ArtifactoryClient
import clients.NexusClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import models.config.ConfigDto
import mu.KotlinLogging
import okhttp3.RequestBody
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import models.artifactory.ArtifactoryFileChecksums
import models.GenericContentItem
import utilities.CustomPool
import utilities.sumByLong
import java.io.File

val config = loadYaml(Paths.get("config.yml"))
val logger = KotlinLogging.logger {}
var erroredOutItems = mutableListOf<GenericContentItem>()
val timestamp = System.currentTimeMillis() / 1000
var args  = CliArgs(ArgParser(emptyArray()), config)

fun main(arguments: Array<String>) = mainBody("java -jar omnibus.jar") {
    args = CliArgs(ArgParser(arguments), config)
    NexusClient.fromRepo = args.fromRepo
    CustomPool.parallelism = args.parallelism.toString().toInt()
    val artifactsList = getArtifactsList(args.fromRepo, args.fromPath, args.artifactsListFile, args.saveList)

    val jars = artifactsList.filter { it.relativePath().endsWith(".jar") }.sortedBy { it.sizeOnDisk() }
    val poms = artifactsList.filter { it.relativePath().endsWith(".pom") }.sortedBy { it.sizeOnDisk() }
    val mavenMetadata = artifactsList.filter { it.relativePath().endsWith("maven-metadata.xml") }.sortedBy { it.sizeOnDisk() }
    val misc = (artifactsList - jars - poms - mavenMetadata)
            .filterNot { it.relativePath().endsWith(".sha1") || it.relativePath().endsWith(".md5") }.sortedBy { it.sizeOnDisk() }

    logger.info { "JARs: ${jars.sumByLong { it.sizeOnDisk() } } bytes in ${jars.size} files found." }
    logger.info { "POMs: ${poms.sumByLong { it.sizeOnDisk() } } bytes in ${poms.size} files found." }
    logger.info {
        "Maven metadata: ${mavenMetadata.sumByLong { it.sizeOnDisk() } } bytes in ${mavenMetadata.size} files found." }
    logger.info { "Miscellaneous: ${misc.sumByLong { it.sizeOnDisk() } } bytes in ${misc.size} files found." }

    val timeToCopyPoms = measureTimeMillis { copyArtifacts(poms, args.destRepo, args.verbose) }
    val timeToCopyMisc = measureTimeMillis { copyArtifacts(misc, args.destRepo, args.verbose) }
    val timeToCopyJars = measureTimeMillis { copyArtifacts(jars, args.destRepo, args.verbose) }
    val timeToCopyMetadata = measureTimeMillis { copyArtifacts(mavenMetadata, args.destRepo, args.verbose, true) }

    logger.info { "Time to copy JARs: $timeToCopyJars ms\n" }
    logger.info { "Time to copy POMs: $timeToCopyPoms ms\n" }
    logger.info { "Time to copy maven metadata: $timeToCopyMetadata ms\n" }
    logger.info { "Time to copy miscellaneous: $timeToCopyMisc ms\n" }

    val timeToWriteJson = measureTimeMillis {
        jacksonObjectMapper().writeValue(File("erroredOutArtifactsList-$timestamp.json"), erroredOutItems)
    }
    logger.info { "Time to save the errored out artifacts list to JSON file: $timeToWriteJson ms\n" }
}

fun copyArtifacts(collection: List<GenericContentItem>, destRepo: String, verbose: Boolean, force: Boolean = false) {
    val deferred = collection.map {
        async(CustomPool.instance) {
            copyArtifact(it, destRepo, verbose, force)
        }
    }
    runBlocking {
        deferred.forEach { it.await() }
    }
}

fun copyArtifact(artifact: GenericContentItem, destRepo: String, verbose: Boolean, force: Boolean = false) {
    try {
        if (!willCopy(artifact, destRepo, verbose, force)) {
            return
        }

        val contentRequest = if (args.sourceRepoType == CliArgs.NEXUS) {
            NexusClient.instance.getFileContent(artifact.relativePath()).execute()
        } else {
            ArtifactoryClient.sourceInstance.getFileContent(artifact.repo + artifact.relativePath()).execute()
        }

        if (contentRequest.code() == 404) {
            logger.info { "[MEH] Stale artifact, doesn't exist in source anymore: ${artifact.relativePath()}. " +
                    "Wasn't transferred." }
            return
        }
        val artifactBytes = contentRequest.body().bytes()
        val requestBody = RequestBody.create(contentRequest.body().contentType(), artifactBytes)
        val deployRequest = ArtifactoryClient.instance.deployArtifact(
                (destRepo + "/" + artifact.relativePath()).replace("//", "/"), artifact.sha1, requestBody).execute()

        if (deployRequest.isSuccessful) {
            logger.info { "[OK] Transferred to Artifactory: ${artifact.relativePath()}" }
        } else {
            val errorString = deployRequest.errorBody().string()
            logger.error { "[FAIL] code: ${deployRequest.code()}, path: ${artifact.relativePath()}," +
                    " message: $errorString" }
            erroredOutItems.add(artifact)
        }
    } catch (_: java.net.SocketTimeoutException) {
        logger.error { "[TIMEOUT] Failed to transfer because of timeout: ${artifact.relativePath()}" }
        erroredOutItems.add(artifact)
    } catch (e: Exception) {
        logger.info { "[NOT_OK] Failed to transfer because of an error: ${artifact.relativePath()}, ${e.message}" }
        erroredOutItems.add(artifact)
    }
}

fun willCopy(artifact: GenericContentItem, destRepo: String, verbose: Boolean, force: Boolean): Boolean {
    val info = ArtifactoryClient.instance.getArtifactInfo((destRepo + "/" + artifact.relativePath()).replace("//", "/")).execute().body()
    var result = true

    if (info != null) {
        if (artifact.sha1 == info.checksums!!.sha1) {
            if (verbose) {
                logger.info(
                        "[SKIP] Artifact with sha1 ${artifact.sha1} already exists in destination, " +
                                artifact.relativePath()
                )
            }
            result = false
        } else {
            if (force) {
                logger.info("[FORCE] Artifact exists but sha1 doesn't match for ${artifact.relativePath()}.")
            } else {
                logger.error("[NO_MATCH] Artifact exists but sha1 doesn't match for ${artifact.relativePath()}")
                result = false
            }
        }
    }
    return result
}

fun getArtifactsList(repo: String, startPath: String, fileName: String, saveList: Boolean): List<GenericContentItem> {
    val file = File(fileName)
    var result = emptyList<GenericContentItem>()

    if (fileName.isNotEmpty() && file.exists()) {
        result = jacksonObjectMapper().readValue(file.readText())
        result = result.filter { it.path!!.startsWith("/$startPath".replace("//", "/")) }
    } else {
        logger.warn { "No file to load the artifacts list from is specified. Recalculating." }
        val timeToBuildList = measureTimeMillis {
            val startItem = GenericContentItem(startPath, startPath, false, 0, 0, repo, ArtifactoryFileChecksums("", ""), emptyList())
            val queue = mutableListOf<GenericContentItem>()
            queue.add(startItem)

            result = getTreeLeaves(queue)
        }
        logger.info { "Time to build the artifacts list: $timeToBuildList ms\n" }

        if (saveList) {
            val timeToWriteJson = measureTimeMillis {
                jacksonObjectMapper().writeValue(File("artifactsList-$timestamp.json"), result)
            }
            logger.info { "Time to save the artifacts list to JSON file: $timeToWriteJson ms\n" }
        }
    }
    return result
}

fun getTreeLeaves(queue: MutableList<GenericContentItem>): List<GenericContentItem> {
    val leaves = mutableListOf<GenericContentItem>()

    while (queue.isNotEmpty()) {
        val current = queue.removeAt(0)

        when (args.sourceRepoType) {
            CliArgs.NEXUS -> {
                val content = NexusClient.instance.getRepoContent(current.relativePath()).execute().body()
                content.data.forEach {
                    if (it.isLeaf()) leaves.add(it) else queue.add(it)
                }
            }
            CliArgs.ARTIFACTORY -> {
                val path = (current.repo + "/" + current.relativePath()).replace("//", "/")
                logger.debug { "packagePath: $path" }
                val response = ArtifactoryClient.sourceInstance.getArtifactInfo(path).execute()
                if (!response.isSuccessful){
                    logger.warn {"Call to host ${config.artifactory.sourceHost} unsuccessful: ${response.message()}"}
                }
                else {
                    val content = response.body()
                    content.children!!.forEach {
                        val seed = GenericContentItem(content.path + it.uri, content.path + it.uri, null, 0, 0, content.repo, ArtifactoryFileChecksums("", ""), emptyList())
                        queue.add(seed)
                    }
                    if (content.isLeaf()) leaves.add(content)
                }
            }
            else -> throw Exception("Unknown source repository type.")
        }
    }
    return leaves
}

fun loadYaml(path: Path): ConfigDto {
    val mapper = ObjectMapper(YAMLFactory())
    mapper.registerModule(KotlinModule())

    return Files.newBufferedReader(path).use {
        mapper.readValue(it, ConfigDto::class.java)
    }
}
