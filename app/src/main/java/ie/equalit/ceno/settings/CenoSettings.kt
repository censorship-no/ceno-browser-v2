package ie.equalit.ceno.settings

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.widget.Toast
import androidx.preference.PreferenceManager
import ie.equalit.ceno.BuildConfig
import ie.equalit.ceno.R
import ie.equalit.ceno.components.ceno.OuinetBroadcastReceiver
import ie.equalit.ceno.ext.cenoPreferences
import ie.equalit.ceno.ext.components
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.Response
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
    STATUS("api/status"),
    PURGE("?purge_cache=do"),
    ORIGIN_ACCESS("?origin_access"),
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

    fun isOriginAccessEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            context.getString(R.string.pref_key_ceno_sources_origin), false
        )

    fun setOriginAccess(context: Context, value: Boolean) {
        val key = context.getString(R.string.pref_key_ceno_sources_origin)
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
        setOriginAccess(context, status.origin_access)
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
                    OuinetKey.STATUS -> {
                        if (response != null)
                            updateOuinetStatus(context, response)
                    }
                    OuinetKey.PURGE -> {
                        val text = if (response != null)
                            context.resources.getString(R.string.clear_cache_success)
                        else
                            context.resources.getString(R.string.clear_cache_fail)
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    }
                    OuinetKey.ORIGIN_ACCESS -> {
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
