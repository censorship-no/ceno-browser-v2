package ie.equalit.ceno.addons

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isGone
import androidx.core.view.isInvisible
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

    private var sourceUrl: String? = null // for retrying in case network call fails

    init {
        initWindow()
        setContentView(binding.root)
        expand()
        updateTitle()
        updateConnectionState()
        setOnClickListener()
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
        engineSession.register(object : EngineSession.Observer {
            override fun onLoadRequest(url: String, triggeredByRedirect: Boolean, triggeredByWebContent: Boolean) {
                super.onLoadRequest(url, triggeredByRedirect, triggeredByWebContent)
                sourceUrl = url.replace("popup.html", "sources.html")
                Logger.debug(sourceUrl)
                sourceUrl?.let { getSources(it) }
            }
        })
    }

    private fun getSources(url: String) {
        binding.progressBar.isGone = false
        binding.failureGroup.isGone = true
        binding.successGroup.isInvisible = false // isInvisible toggle between View.INVISIBLE and View.VISIBLE
        MainScope().launch {
            webClientRequest(Request(url)).let { response ->
                if (response != null) {
                    binding.progressBar.isGone = true
                    binding.tvDirectFromWebsiteCount.text = response.origin ?: "0"
                    binding.tvPersonalNetworkCount.text = response.proxy ?: "0"
                    binding.tvPublicNetworkCount.text = response.injector ?: "0"
                    binding.tvSharedByOthersCount.text = response.distCache ?: "0"
                    binding.tvSharedByYouCount.text = response.localCache ?: "0"
                } else {
                    // display error view that can trigger a retry of the API call
                    binding.progressBar.isGone = true
                    binding.failureGroup.isGone = false
                    binding.successGroup.isInvisible = true
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
                Logger.debug(ex.toString())
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

    private fun setOnClickListener() {
        binding.btnRetry.setOnClickListener {
            sourceUrl?.let { getSources(it) }
        }
    }
}
