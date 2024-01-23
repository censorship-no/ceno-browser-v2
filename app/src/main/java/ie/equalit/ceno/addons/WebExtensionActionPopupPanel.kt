package ie.equalit.ceno.addons

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isGone
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.DialogWebExtensionPopupSheetBinding
import ie.equalit.ceno.ext.components
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.concept.engine.EngineSession
import mozilla.components.support.ktx.android.view.putCompoundDrawablesRelativeWithIntrinsicBounds
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import org.json.JSONObject


class WebExtensionActionPopupPanel(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val tabUrl: String,
    private val isConnectionSecure: Boolean,
    private val cachedSourceCounts: JSONObject?
) : BottomSheetDialog(context), EngineSession.Observer {

    private var binding: DialogWebExtensionPopupSheetBinding =
        DialogWebExtensionPopupSheetBinding.inflate(layoutInflater, null, false)

    init {
        initWindow()
        setContentView(binding.root)
        expand()
        updateTitle()
        updateConnectionState()
        cachedSourceCounts?.let { c -> onCountsFetched(c) }
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

    fun onCountsFetched(response: JSONObject) {

        if (response.has("url") && response.getString("url") == tabUrl.tryGetHostFromUrl()) {
            binding.progressBar.isGone = context.components.core.store.state.selectedTab?.content?.loading == false

            binding.tvDirectFromWebsiteCount.text = if (response.has("origin")) response.getString("origin") else "0"
            binding.tvSharedByOthersCount.text = if (response.has("dist-cache")) response.getString("dist-cache") else "0"
            binding.tvSharedByYouCount.text = if (response.has("local-cache")) response.getString("local-cache") else "0"

            val proxy = if (response.has("proxy")) response.getString("proxy").toInt() else 0
            val injector = if (response.has("injector")) response.getString("injector").toInt() else 0

            binding.tvViaCenoNetworkCount.text = (proxy.plus(injector)).toString()
        }
    }
}
