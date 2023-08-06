package ie.equalit.ceno.settings

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import ie.equalit.ceno.BuildConfig
import ie.equalit.ceno.R
import ie.equalit.ceno.ext.components
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mozilla.components.concept.fetch.Request
import mozilla.components.support.base.log.logger.Logger
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow


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
    GROUPS_TXT("groups.txt"),
    LOGFILE("?logfile"),
}

enum class OuinetValue(val string: String) {
    DISABLED("disabled"),
    ENABLED("enabled")
}

object CenoSettings {

    const val SET_VALUE_ENDPOINT = "http://127.0.0.1:" + BuildConfig.FRONTEND_PORT
    const val LOGFILE_TXT = "logfile.txt"

    private fun log2(n: Int): Double {
        return ln(n.toDouble()) / ln(2.0)
    }

    private fun bytesToString(b: Int): String {
        // originally from <https://stackoverflow.com/a/42408230>
        // ported from extension JS code to kotlin
        if (b == 0) {
            return "0 B"
        }
        val i = floor(log2(b) / 10).toInt()
        val v = b / 1024.0.pow(i)
        val u =
            if (i > 0)
                "KMGTPEZY"[i - 1] + "iB"
            else
                "B"
        return String.format("%.2f %s", v, u)
    }

    fun getCenoVersionString(context: Context) : String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            String.format(
                "%s Build ID %s",
                packageInfo.versionName,
                packageInfo.versionCode,
            )
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
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
        CustomPreferenceManager.setString(context, R.string.pref_key_ouinet_state, status.state)
        CustomPreferenceManager.setBoolean(context, R.string.pref_key_ceno_sources_origin, status.origin_access)
        CustomPreferenceManager.setBoolean(context, R.string.pref_key_ceno_sources_private, status.proxy_access)
        CustomPreferenceManager.setBoolean(context, R.string.pref_key_ceno_sources_public, status.injector_access)
        CustomPreferenceManager.setBoolean(context, R.string.pref_key_ceno_sources_shared, status.distributed_cache)
        CustomPreferenceManager.setString(context, R.string.pref_key_ceno_cache_size, bytesToString(status.local_cache_size!!))
        CustomPreferenceManager.setString(context, R.string.pref_key_ouinet_version, status.ouinet_version)
        CustomPreferenceManager.setString(context, R.string.pref_key_ouinet_build_id, status.ouinet_build_id)
        CustomPreferenceManager.setInt(context, R.string.pref_key_ouinet_protocol, status.ouinet_protocol)
        CustomPreferenceManager.setBoolean(context, R.string.pref_key_ceno_enable_log, status.logfile)
        context.components.cenoPreferences.sharedPrefsReload = true
    }

    private fun updateCenoGroups(context : Context, responseBody : String) {
        Logger.debug("Response body: $responseBody")
        val groups = responseBody.reader().readLines()
        CustomPreferenceManager.setInt(context, R.string.pref_key_ceno_groups_count, groups.count())
        context.components.cenoPreferences.sharedPrefsUpdate = true
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
                        val text = if (response != null) {
                            CustomPreferenceManager.setString(context, R.string.pref_key_ceno_cache_size, bytesToString(0))
                            CustomPreferenceManager.setInt(context, R.string.pref_key_ceno_groups_count, 0)
                            context.components.cenoPreferences.sharedPrefsUpdate = true
                            context.resources.getString(R.string.clear_cache_success)
                        }
                        else {
                            context.resources.getString(R.string.clear_cache_fail)
                        }
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    }
                    OuinetKey.ORIGIN_ACCESS,
                    OuinetKey.PROXY_ACCESS,
                    OuinetKey.INJECTOR_ACCESS,
                    OuinetKey.DISTRIBUTED_CACHE,
                    OuinetKey.LOGFILE,
                    -> {
                        if (response == null) {
                            Toast.makeText(
                                context,
                                context.resources.getString(R.string.ouinet_client_fetch_fail),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else {
                            if (key == OuinetKey.LOGFILE) {
                                context.components.cenoPreferences.sharedPrefsUpdate = true
                            }
                        }
                    }
                    OuinetKey.GROUPS_TXT -> {
                        if (response != null)
                            updateCenoGroups(context, response)
                    }
                }
            }
            return@launch
        }
    }
}
