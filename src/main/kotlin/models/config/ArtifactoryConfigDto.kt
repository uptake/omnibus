package models.config

data class ArtifactoryConfigDto(val host: String, val defaultRepository: String, val apiKey: String,
                                val sourceHost: String?)
