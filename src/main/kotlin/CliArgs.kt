package com.uptake.omnibus

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import models.config.ConfigDto

class CliArgs(parser: ArgParser, config: ConfigDto) {
    companion object {
        const val NEXUS = "nexus"
        const val ARTIFACTORY = "artifactory"
    }

    val fromRepo by parser.storing("-f", "--from-repo", help = "Name of the source repo to copy. If not specified, " +
            "the \"nexus.defaultRepository\" value from `config.yml` is taken")
            .default(config.nexus.defaultRepository)
    val fromPath by parser.storing("-p", "--from-path", help = "Name of the source folder to copy. If not specified," +
            " all artifacts under the specified source will be copied.")
            .default("")
    val destRepo by parser.storing("-d", "--dest-repo", help = "Name of the destination repo. If not specified, " +
            "the \"artifactory.defaultRepository\" value from `config.yml` is taken")
            .default(config.artifactory.defaultRepository)
    val artifactsListFile by parser.storing("-a", "--artifacts-list",
            help = "Name of the artifacts list JSON dump. If not specified, current list of artifacts will be dumped " +
                    "in a JSON file with current timestamp, e.g. artifactsList-1504196592.json")
            .default("")
    val parallelism by parser.storing("--parallelism", help = "Number of coroutines running in parallel.")
            .default(config.parallelism)
    val sourceRepoType by parser.storing("--source-repo-type", help = "Type of source repository: [artifactory, nexus]")
            .default(NEXUS)
    val saveList by parser.flagging("-s", "--save-list", help = "Whether to save the list of artifacts to a JSON file, defaults to false.")
            .default(false)
    val verbose by parser.flagging("-v", "--verbose", help = "Verbose output, prints [SKIP] log messages.")
            .default(false)
}
