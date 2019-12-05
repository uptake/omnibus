package models.nexus

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NexusFileInfo(
        val size: Int,
        val sha1Hash: String
)
