package ie.equalit.ceno.util_background_runner.model

import kotlinx.serialization.Serializable

@Serializable
data class BackgroundResponse(
    val statusCode: Int = 400,
    val url: String = "",
)