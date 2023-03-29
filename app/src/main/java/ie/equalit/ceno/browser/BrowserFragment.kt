/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.browser

import android.os.Bundle
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ie.equalit.ceno.R
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.feature.readerview.view.ReaderViewControlsBar
//import mozilla.components.feature.toolbar.WebExtensionToolbarFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
//import ie.equalit.ceno.getComponents
import ie.equalit.ceno.ext.requireComponents
import ie.equalit.ceno.settings.Settings
import ie.equalit.ceno.tabs.TabsTrayFragment

/**
 * Fragment used for browsing the web within the main app.
 */
class BrowserFragment : BaseBrowserFragment(), UserInteractionHandler {
    private val readerViewFeature = ViewBoundFeatureWrapper<ReaderViewIntegration>()
    /* Removing WebExtension toolbar feature, see below for more details
    private val webExtToolbarFeature = ViewBoundFeatureWrapper<WebExtensionToolbarFeature>()
     */

    private val toolbar: BrowserToolbar
        get() = requireView().findViewById(R.id.toolbar)
    private val readerViewBar: ReaderViewControlsBar
        get() = requireView().findViewById(R.id.readerViewBar)
    private val readerViewAppearanceButton: FloatingActionButton
        get() = requireView().findViewById(R.id.readerViewAppearanceButton)

    /*
    override val shouldUseComposeUI: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(
            getString(R.string.pref_key_compose_ui),
            false,
        )
     */

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val homeAction = BrowserToolbar.Button(
            imageDrawable = ResourcesCompat.getDrawable(
                resources,
                R.drawable.mozac_ic_home,
                null
            )!!,
            contentDescription = requireContext().getString(R.string.browser_toolbar_home),
            iconTintColorResource = R.color.fx_mobile_text_color_primary,
            listener = ::onHomeButtonClicked,
        )


        if (Settings.shouldShowHomeButton(requireContext())) {
            toolbar.addNavigationAction(homeAction)
        }

        readerViewFeature.set(
            feature = ReaderViewIntegration(
                requireContext(),
                requireComponents.core.engine,
                requireComponents.core.store,
                toolbar,
                readerViewBar,
                readerViewAppearanceButton,
            ),
            owner = this,
            view = view,
        )

        /*
         * Remove WebExtension toolbar feature because
         * we don't want the browserAction button in toolbar and
         * the pageAction button created by it didn't work anyway
         */
        /*
        webExtToolbarFeature.set(
            feature = WebExtensionToolbarFeature(
                toolbar,
                requireContext().components.core.store,
            ),
            owner = this,
            view = view,
        )
        */
        binding.homeAppBar.visibility = View.GONE
        binding.sessionControlRecyclerView.visibility = View.GONE
        binding.swipeRefresh.visibility = View.VISIBLE
    }

    private fun showTabs() {
        // For now we are performing manual fragment transactions here. Once we can use the new
        // navigation support library we may want to pass navigation graphs around.
        /* CENO: Add this transaction to back stack to go back to correct fragment on back pressed */
        activity?.supportFragmentManager?.beginTransaction()?.apply {
            replace(R.id.container, TabsTrayFragment(), TabsTrayFragment.TAG)
            commit()
        }
    }

    private fun onHomeButtonClicked() {
        /* Request home page url, toolbar listener will take care of the home fragment transaction */
        requireComponents.useCases.sessionUseCases.loadUrl(CenoHomeFragment.ABOUT_HOME)
    }

    override fun onBackPressed(): Boolean =
        readerViewFeature.onBackPressed() || super.onBackPressed()

    companion object {
        /* CENO: Add a tag to keep track of whether this fragment is open */
        const val TAG = "BROWSER"
        fun create(sessionId: String? = null) = BrowserFragment().apply {
            arguments = Bundle().apply {
                putSessionId(sessionId)
            }
        }
    }
}
