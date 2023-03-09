package ie.equalit.ceno.settings

import android.content.Context
import android.widget.Toast
import androidx.preference.PreferenceManager
import ie.equalit.ceno.BuildConfig
import ie.equalit.ceno.R
import ie.equalit.ceno.ext.components
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mozilla.components.concept.fetch.Request
import mozilla.components.support.base.log.logger.Logger


@Serializable
data class OuinetStatus(val auto_refresh : Boolean,
                        val bt_extra_bootstraps : Array<String>,
                        val distributed_cache : Boolean,
                        val external_udp_endpoints : Array<String>? = null,
                        val injector_access : Boolean,
                        val is_upnp_active : String,
                        val local_cache_size : Int? = null,
                        val local_udp_endpoints : Array<String>? = null,
                        val logfile : Boolean,
                        val max_cached_age : Int,
                        val origin_access : Boolean,
                        val ouinet_build_id : String,
                        val ouinet_protocol : Int,
                        val ouinet_version: String,
                        val proxy_access : Boolean,
                        val public_udp_endpoints: Array<String>? = null,
                        val state: String,
                        val udp_world_reachable : String? = null
)

enum class OuinetKey(val command : String) {
    API_STATUS("api/status"),
    PURGE_CACHE("?purge_cache=do"),
    ORIGIN_ACCESS("?origin_access"),
    PROXY_ACCESS("?proxy_access"),
    INJECTOR_ACCESS("?injector_access"),
    DISTRIBUTED_CACHE("?distributed_cache"),
}

enum class OuinetValue(val string: String) {
    DISABLED("disabled"),
    ENABLED("enabled")
}

object CenoSettings {

    private const val SET_VALUE_ENDPOINT = "http://127.0.0.1:" + BuildConfig.FRONTEND_PORT

    fun isStatusUpdateRequired(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            context.getString(R.string.pref_key_ceno_status_update_required), false
        )

    fun setStatusUpdateRequired(context: Context, value: Boolean) {
        val key = context.getString(R.string.pref_key_ceno_status_update_required)
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    private fun setCenoSourcesOrigin(context: Context, value: Boolean) {
        val key = context.getString(R.string.pref_key_ceno_sources_origin)
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    private fun setCenoSourcesPrivate(context: Context, value: Boolean) {
        val key = context.getString(R.string.pref_key_ceno_sources_private)
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    private fun setCenoSourcesPublic(context: Context, value: Boolean) {
        val key = context.getString(R.string.pref_key_ceno_sources_public)
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    private fun setCenoSourcesShared(context: Context, value: Boolean) {
        val key = context.getString(R.string.pref_key_ceno_sources_shared)
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    private suspend fun webClientRequest (context: Context, request: Request): String? {
        var responseBody : String? = null
        var tries = 0
        var success = false
        while (tries < 5 && !success) {
            try {
                context.components.core.client.fetch(request).use { response ->
                    if (response.status == 200) {
                        Logger.debug("webClientRequest succeeded try $tries")
                        Logger.debug("Response header: ${response.headers}")
                        responseBody = response.body.string()
                        success = true
                    } else {
                        tries++
                        Logger.debug("Clear cache failed on try $tries")
                        delay(500)
                    }
                }
            } catch (ex: Exception) {
                tries++
                Logger.debug("Clear cache failed on try $tries")
                delay(500)
            }
        }
        return responseBody
    }

    private fun updateOuinetStatus(context : Context, responseBody : String) {
        val status = Json.decodeFromString<OuinetStatus>(responseBody)
        Logger.debug("Response body: $status")
        setCenoSourcesOrigin(context, status.origin_access)
        setCenoSourcesPrivate(context, status.proxy_access)
        setCenoSourcesPublic(context, status.injector_access)
        setCenoSourcesShared(context, status.distributed_cache)
        context.components.cenoPreferences.statusUpdateComplete = true
    }

    fun ouinetClientRequest(context: Context, key : OuinetKey, newValue: OuinetValue? = null) {
        MainScope().launch {
            val request : String = if (newValue != null)
                "${SET_VALUE_ENDPOINT}/${key.command}=${newValue.string}"
            else
                "${SET_VALUE_ENDPOINT}/${key.command}"

            webClientRequest(context, Request(request)).let { response ->
                when (key) {
                    OuinetKey.API_STATUS -> {
                        if (response != null)
                            updateOuinetStatus(context, response)
                    }
                    OuinetKey.PURGE_CACHE -> {
                        val text = if (response != null)
                            context.resources.getString(R.string.clear_cache_success)
                        else
                            context.resources.getString(R.string.clear_cache_fail)
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    }
                    OuinetKey.ORIGIN_ACCESS,
                    OuinetKey.PROXY_ACCESS,
                    OuinetKey.INJECTOR_ACCESS,
                    OuinetKey.DISTRIBUTED_CACHE,
                    -> {
                        if (response == null) {
                            Toast.makeText(
                                context,
                                context.resources.getString(R.string.ouinet_client_fetch_fail),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            return@launch
        }
    }
}
