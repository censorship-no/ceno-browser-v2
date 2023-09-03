package ie.equalit.ceno.settings


data class CenoSources(
    val origin: String? = null,
    val injector: String? = null,
    val proxy: String? = null,
    val distCache: String? = null,
    val localCache: String? = null,
)