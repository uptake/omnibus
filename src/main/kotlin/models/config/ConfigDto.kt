package models.config

data class ConfigDto(val nexus: NexusConfigDto, val artifactory: ArtifactoryConfigDto, val parallelism: Int)
