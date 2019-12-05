package models.artifactory

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArtifactoryApiError(val status: Int, val message: String)
