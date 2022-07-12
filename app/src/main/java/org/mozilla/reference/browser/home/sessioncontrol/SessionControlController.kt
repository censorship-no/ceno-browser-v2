/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser.home.sessioncontrol

import android.annotation.SuppressLint
/*
import kotlinx.coroutines.CoroutineScope
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.tabs.TabsUseCases
 */
import mozilla.components.feature.top.sites.TopSite
import org.mozilla.reference.browser.BrowserActivity

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
     * @see [TopSiteInteractor.onSettingsClicked]
     */
    fun handleTopSiteSettingsClicked()

    /**
     * @see [CollectionInteractor.onCollectionMenuOpened] and [TopSiteInteractor.onTopSiteMenuOpened]
     */
    fun handleMenuOpened()

}

@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
class DefaultSessionControlController(
    private val activity: BrowserActivity,
    /*
    private val engine: Engine,
    private val store: BrowserStore,
    private val addTabUseCase: TabsUseCases.AddNewTabUseCase,
    private val viewLifecycleScope: CoroutineScope,
     */
) : SessionControlController {

    override fun handleMenuOpened() {
        //dismissSearchDialogIfDisplayed()
    }

    /* TODO: Currently only selecting the site only does anything, fix rename and remove options */
    @SuppressLint("InflateParams")
    override fun handleRenameTopSiteClicked(topSite: TopSite) {
        /*
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
                        with(activity.components.useCases.topSitesUseCase) {
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
         */
    }

    override fun handleRemoveTopSiteClicked(topSite: TopSite) {
        /*
        when (topSite.url) {
            SupportUtils.POCKET_TRENDING_URL -> Pocket.pocketTopSiteRemoved.record(NoExtras())
            SupportUtils.GOOGLE_URL -> TopSites.googleTopSiteRemoved.record(NoExtras())
            SupportUtils.BAIDU_URL -> TopSites.baiduTopSiteRemoved.record(NoExtras())
        }

        viewLifecycleScope.launch(Dispatchers.IO) {
            with(activity.components.useCases.topSitesUseCase) {
                removeTopSites(topSite)
            }
        }
         */
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

    override fun handleTopSiteSettingsClicked() {
        /*
        navController.nav(
            R.id.homeFragment,
            HomeFragmentDirections.actionGlobalHomeSettingsFragment()
        )
         */
    }
}
