package models.artifactory

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArtifactoryFileChecksums(val sha1: String, val md5: String)
