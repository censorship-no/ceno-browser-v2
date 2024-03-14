package ie.equalit.ceno.addons

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isGone
import androidx.core.view.isVisible
import ie.equalit.ceno.ext.createSegment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import ie.equalit.ceno.R
import ie.equalit.ceno.browser.BaseBrowserFragment
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
    cachedSourceCounts: JSONObject?
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

        if (response.has(BaseBrowserFragment.URL) && response.getString(BaseBrowserFragment.URL) == tabUrl.tryGetHostFromUrl()) {
            binding.progressBar.isGone = context.components.core.store.state.selectedTab?.content?.loading == false

//            binding.tvSharedByYouCount.text = if (response.has("local-cache")) response.getString("local-cache") else "0"

            val distCache = if (response.has(BaseBrowserFragment.DIST_CACHE)) response.getString(BaseBrowserFragment.DIST_CACHE).toFloat() else 0F
            val proxy = if (response.has(BaseBrowserFragment.PROXY)) response.getString(BaseBrowserFragment.PROXY).toFloat() else 0F
            val injector = if (response.has(BaseBrowserFragment.INJECTOR)) response.getString(BaseBrowserFragment.INJECTOR).toFloat() else 0F
            val origin = if (response.has(BaseBrowserFragment.ORIGIN)) response.getString(BaseBrowserFragment.ORIGIN).toFloat() else 0F

            binding.tvViaCenoNetworkCount.text = (proxy.plus(injector).plus(distCache)).toInt().toString()
            binding.tvDirectFromWebsiteCount.text = origin.toInt().toString()


            val sum = distCache + origin + injector + proxy
            binding.sourcesProgressBar.isVisible = sum != 0F

            binding.sourcesProgressBar.removeAllViews()

            if(origin > 0) binding.sourcesProgressBar.addView(context.createSegment(((origin / sum) * 100), R.color.ceno_sources_green))
            if((proxy + injector + distCache) > 0) binding.sourcesProgressBar.addView(context.createSegment((((proxy + injector + distCache) / sum) * 100), R.color.ceno_sources_orange))
//                if(localCache > 0) binding.sourcesProgressBar.addView(createSegment((localCache / sum) * 100, R.color.ceno_sources_yellow))
        }
    }
}
