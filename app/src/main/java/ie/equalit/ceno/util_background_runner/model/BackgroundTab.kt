package ie.equalit.ceno.util_background_runner.model

import kotlinx.serialization.Serializable

@Serializable
data class BackgroundTab(
    val tabId: Int = 0,
    val url: String = "",
    val requestHeaders: MutableMap<String, String?> = mutableMapOf()
)