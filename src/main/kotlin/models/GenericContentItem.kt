package models

import clients.NexusClient
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import models.artifactory.ArtifactoryFileChecksums
import models.artifactory.ArtifactoryStorageChild

@JsonIgnoreProperties(ignoreUnknown = true)
data class GenericContentItem(
        val relativePath: String?,
        val path: String?,
        val leaf: Boolean?,
        val sizeOnDisk: Long?,
        val size: Long?,
        val repo: String?,
        val checksums: ArtifactoryFileChecksums?,
        val children: List<ArtifactoryStorageChild>? = emptyList()
) {
    fun relativePath(): String {
        return if (relativePath == null) "" + path else "" + relativePath
    }

    fun isLeaf(): Boolean {
        return if (leaf == null) children!!.size == 0 else leaf
    }

    fun sizeOnDisk(): Long {
        return when {
            sizeOnDisk != null -> sizeOnDisk
            size != null -> size
            else -> 0
        }
    }

    val sha1: String by lazy {
        if (checksums != null) {
            checksums.sha1
        } else {
            val fileInfo = NexusClient.instance.getFileInfo(relativePath()).execute().body()
            fileInfo.data.sha1Hash
        }
    }
}
