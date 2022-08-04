/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser.home.sessioncontrol

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
/*
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.tabs.TabsUseCases
 */
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.support.ktx.android.view.showKeyboard
import org.mozilla.reference.browser.BrowserActivity
import org.mozilla.reference.browser.R
import org.mozilla.reference.browser.ext.components

/**
 * [HomeFragment] controller. An interface that handles the view manipulation of the Tabs triggered
 * by the Interactor.
 */
@Suppress("TooManyFunctions")
interface SessionControlController {
    /**
     * @see [TopSiteInteractor.onRenameTopSiteClicked]
     */
    fun handleRenameTopSiteClicked(topSite: TopSite)

    /**
     * @see [TopSiteInteractor.onRemoveTopSiteClicked]
     */
    fun handleRemoveTopSiteClicked(topSite: TopSite)

    /**
     * @see [TopSiteInteractor.onSelectTopSite]
     */
    fun handleSelectTopSite(topSite: TopSite, position: Int)

    /**
     * @see [TopSiteInteractor.onOpenInPrivateTabClicked]
     */
    fun handleOpenInPrivateTabClicked(topSite: TopSite)

    /**
     * @see [TopSiteInteractor.onSettingsClicked]
     */
    fun handleTopSiteSettingsClicked()

    /**
     * @see [CollectionInteractor.onCollectionMenuOpened] and [TopSiteInteractor.onTopSiteMenuOpened]
     */
    fun handleMenuOpened()

    fun handleCenoModeClicked()
}

@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
class DefaultSessionControlController(
    private val activity: BrowserActivity,
    /*
    private val engine: Engine,
    private val store: BrowserStore,
    private val addTabUseCase: TabsUseCases.AddNewTabUseCase,
     */
    private val viewLifecycleScope: CoroutineScope,
) : SessionControlController {

    override fun handleMenuOpened() {
        //dismissSearchDialogIfDisplayed()
    }

    @SuppressLint("InflateParams")
    override fun handleRenameTopSiteClicked(topSite: TopSite) {
        activity.let {
            val customLayout =
                LayoutInflater.from(it).inflate(R.layout.top_sites_rename_dialog, null)
            val topSiteLabelEditText: EditText =
                customLayout.findViewById(R.id.top_site_title)
            topSiteLabelEditText.setText(topSite.title)

            AlertDialog.Builder(it).apply {
                setTitle(R.string.rename_top_site)
                setView(customLayout)
                setPositiveButton(R.string.top_sites_rename_dialog_ok) { dialog, _ ->
                    viewLifecycleScope.launch(Dispatchers.IO) {
                        with(activity.components.useCases.cenoTopSitesUseCase) {
                            updateTopSites(
                                topSite,
                                topSiteLabelEditText.text.toString(),
                                topSite.url
                            )
                        }
                    }
                    dialog.dismiss()
                }
                setNegativeButton(R.string.top_sites_rename_dialog_cancel) { dialog, _ ->
                    dialog.cancel()
                }
            }.show().also {
                topSiteLabelEditText.setSelection(0, topSiteLabelEditText.text.length)
                topSiteLabelEditText.showKeyboard()
            }
        }
    }

    override fun handleRemoveTopSiteClicked(topSite: TopSite) {
        viewLifecycleScope.launch(Dispatchers.IO) {
            with(activity.components.useCases.cenoTopSitesUseCase) {
                removeTopSites(topSite)
            }
        }
    }

    override fun handleSelectTopSite(topSite: TopSite, position: Int) {
        //dismissSearchDialogIfDisplayed()
        /*
        when (topSite.url) {
            SupportUtils.GOOGLE_URL -> TopSites.openGoogleSearchAttribution.record(NoExtras())
            SupportUtils.BAIDU_URL -> TopSites.openBaiduSearchAttribution.record(NoExtras())
            SupportUtils.POCKET_TRENDING_URL -> Pocket.pocketTopSiteClicked.record(NoExtras())
        }

        val availableEngines: List<SearchEngine> = getAvailableSearchEngines()
        val searchAccessPoint = MetricsUtils.Source.TOPSITE

        availableEngines.firstOrNull { engine ->
            engine.resultUrls.firstOrNull { it.contains(topSite.url) } != null
        }?.let { searchEngine ->
            MetricsUtils.recordSearchMetrics(
                searchEngine,
                searchEngine == store.state.search.selectedOrDefaultSearchEngine,
                searchAccessPoint
            )
        }
         */

        /*
        val tabId = addTabUseCase.invoke(
            url = appendSearchAttributionToUrlIfNeeded(topSite.url),
            selectTab = true,
            startLoading = true
        )
         */

        /*
        if (settings.openNextTabInDesktopMode) {
            activity.handleRequestDesktopMode(tabId)
        }
         */
        activity.openToBrowser(topSite.url)
    }

    override fun handleOpenInPrivateTabClicked(topSite: TopSite) {
        with(activity) {
            openToBrowser(
                url = topSite.url,
                newTab = true,
                private = true
            )
        }
    }

    override fun handleTopSiteSettingsClicked() {
        /*
        navController.nav(
            R.id.homeFragment,
            HomeFragmentDirections.actionGlobalHomeSettingsFragment()
        )
         */
    }

    override fun handleCenoModeClicked() {
        activity.apply{
            openToBrowser(getString(R.string.ceno_mode_manual_link))
        }
    }
}
