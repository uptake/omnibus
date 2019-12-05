package models.artifactory

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArtifactoryErrorResponse(val errors: List<ArtifactoryApiError> = emptyList())
