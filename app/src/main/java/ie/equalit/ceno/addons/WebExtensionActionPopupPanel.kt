package ie.equalit.ceno.addons

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.DialogWebExtensionPopupSheetBinding
import ie.equalit.ceno.ext.components
import ie.equalit.ceno.settings.CenoSources
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mozilla.components.browser.icons.IconRequest
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.fetch.Request
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.view.putCompoundDrawablesRelativeWithIntrinsicBounds
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import org.json.JSONObject

@SuppressWarnings("LongParameterList")
class WebExtensionActionPopupPanel(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val tabUrl: String,
    private val isConnectionSecure: Boolean,
) : BottomSheetDialog(context), EngineSession.Observer {

    private var binding: DialogWebExtensionPopupSheetBinding =
        DialogWebExtensionPopupSheetBinding.inflate(layoutInflater, null, false)

    private var session: EngineSession? = null

    init {
        initWindow()
        setContentView(binding.root)
        expand()
        updateTitle()
        updateConnectionState()
    }

    private fun initWindow() {
        this.window?.decorView?.let {
            it.setViewTreeLifecycleOwner(lifecycleOwner)
            it.setViewTreeSavedStateRegistryOwner(
                lifecycleOwner as SavedStateRegistryOwner,
            )
        }
    }

    private fun expand() {
        val bottomSheet =
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
        //bottomSheet.setBackgroundColor(ContextCompat.getColor(context, R.color.fx_mobile_layer_color_1))
        //bottomSheet.background = ContextCompat.getDrawable(context, R.drawable.home_background)
        BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun updateTitle() {
        binding.siteTitle.text = tabUrl.tryGetHostFromUrl()
        context.components.core.icons.loadIntoView(
            binding.siteFavicon,
            IconRequest(tabUrl, isPrivate = true),
        )
    }

    private fun updateConnectionState() {
        binding.securityInfo.text = if (isConnectionSecure) {
            context.getString(R.string.secure_connection)
        } else {
            context.getString(R.string.insecure_connection)
        }

        // TODO: if more info about HTTPS is added, bring back this icon to indicate more info is available
        //val nextIcon = AppCompatResources.getDrawable(context, R.drawable.mozac_ic_arrowhead_right)

        val securityIcon = if (isConnectionSecure) {
            AppCompatResources.getDrawable(context, R.drawable.mozac_ic_lock)
        } else {
            AppCompatResources.getDrawable(context, R.drawable.mozac_ic_warning)
        }

        binding.securityInfo.putCompoundDrawablesRelativeWithIntrinsicBounds(
            start = securityIcon,
            end = null,
            top = null,
            bottom = null,
        )
    }

    fun renderSettingsView(engineSession: EngineSession) {
        session = engineSession
        session?.register(this)
    }

    private fun getSources(url: String) {
        MainScope().launch {
            webClientRequest(Request(url)).let { response ->
                if (response != null) {
                    // load UI state
                } else {
                    // display error view that can trigger a retry of the API call
                }
            }
        }
    }

    private suspend fun webClientRequest(request: Request): CenoSources? {
        var responseBody: CenoSources? = null
        var tries = 0
        var success = false
        while (tries < 5 && !success) {
            try {
                context.components.core.client.fetch(request).use { response ->
                    if (response.status == 200) {
                        Logger.debug("webClientRequest succeeded try $tries")
                        Logger.debug("Response header: ${response.headers}")
                        responseBody = parseJson(response.body.string())
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


    private fun parseJson(jsonString: String): CenoSources? {
        return try {
            val jsonObject = JSONObject(jsonString)

            CenoSources(
                origin = jsonObject.getString("origin"),
                injector = jsonObject.getString("injector"),
                proxy = jsonObject.getString("proxy"),
                distCache = jsonObject.getString("dist-cache"),
                localCache = jsonObject.getString("local-cache")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onLoadRequest(url: String, triggeredByRedirect: Boolean, triggeredByWebContent: Boolean) {
        super.onLoadRequest(url, triggeredByRedirect, triggeredByWebContent)
        session?.unregister(this)
        getSources(url.replace("popup.html", "sources.html"))
    }
}
