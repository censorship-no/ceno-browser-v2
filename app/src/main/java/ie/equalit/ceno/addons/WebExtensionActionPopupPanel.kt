package ie.equalit.ceno.addons

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isGone
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.DialogWebExtensionPopupSheetBinding
import ie.equalit.ceno.ext.components
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.browser.icons.IconRequest
import mozilla.components.concept.engine.EngineSession
import mozilla.components.support.ktx.android.view.putCompoundDrawablesRelativeWithIntrinsicBounds
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import org.json.JSONObject
import org.mozilla.geckoview.WebExtension

@SuppressWarnings("LongParameterList")
class WebExtensionActionPopupPanel(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val tabUrl: String,
    private val isConnectionSecure: Boolean,
) : BottomSheetDialog(context), EngineSession.Observer {

    private var binding: DialogWebExtensionPopupSheetBinding =
        DialogWebExtensionPopupSheetBinding.inflate(layoutInflater, null, false)

    private lateinit var runnable: Runnable
    private var handler = Handler(Looper.getMainLooper())

    init {
        initWindow()
        setContentView(binding.root)
        expand()
        updateTitle()
        updateConnectionState()
        updateStats()

        runnable = Runnable {
            lifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    updateStats()
                    handler.postDelayed(runnable, SOURCES_COUNT_FETCH_DELAY)
                }
            }
        }

        handler.postDelayed(runnable, SOURCES_COUNT_FETCH_DELAY)
    }

    private val portDelegate: WebExtension.PortDelegate = object : WebExtension.PortDelegate {
        override fun onPortMessage(
            message: Any, port: WebExtension.Port
        ) {
            Log.d("PortDelegate", "Received message from extension: $message")

            // nullable check for `message` which returns as null sometimes
            (message as String?)?.let {
                binding.progressBar.isGone = true
                val response = JSONObject(message)

                binding.tvDirectFromWebsiteCount.text = if(response.has("origin")) response.getString("origin") else "0"
                binding.tvPersonalNetworkCount.text = if(response.has("proxy")) response.getString("proxy") else "0"
                binding.tvPublicNetworkCount.text = if(response.has("injector")) response.getString("injector") else "0"
                binding.tvSharedByOthersCount.text = if(response.has("dist-cache")) response.getString("dist-cache") else "0"
                binding.tvSharedByYouCount.text = if(response.has("local-cache")) response.getString("local-cache") else "0"
            }
        }

        override fun onDisconnect(port: WebExtension.Port) {
            // This port is not usable anymore.
            if (port === context.components.webExtensionPort.mPort) {
                context.components.webExtensionPort.mPort = null
            }
        }
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

    private fun updateStats() {

        Log.d("Message", "Updating stats?")
        context.components.webExtensionPort.mPort?.let {
            it.setDelegate(portDelegate)
            val message = JSONObject()
            message.put("requestSources", "true")
            Log.d("Message", "Sending message: $message")
            it.postMessage(message)
        }
    }

    override fun dismiss() {
        super.dismiss()
        handler.removeCallbacks(runnable)
    }

    companion object {
        private const val SOURCES_COUNT_FETCH_DELAY = 7000L
    }
}
