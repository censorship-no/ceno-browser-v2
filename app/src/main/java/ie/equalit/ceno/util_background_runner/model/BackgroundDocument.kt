package ie.equalit.ceno.util_background_runner.model

import kotlinx.serialization.Serializable

@Serializable
data class BackgroundDocument(
    val documentUrl: String?,
    val url: String?,
)