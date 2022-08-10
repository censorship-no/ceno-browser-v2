/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.cenoV2.browser

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import ie.equalit.cenoV2.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.menu2.BrowserMenuController
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.display.DisplayToolbar
import mozilla.components.concept.menu.MenuController
import mozilla.components.concept.menu.candidate.CompoundMenuCandidate
import mozilla.components.concept.menu.candidate.ContainerStyle
import mozilla.components.concept.menu.candidate.DrawableMenuIcon
import mozilla.components.concept.menu.candidate.MenuCandidate
import mozilla.components.concept.menu.candidate.RowMenuCandidate
import mozilla.components.concept.menu.candidate.SmallMenuCandidate
import mozilla.components.concept.menu.candidate.TextMenuCandidate
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.feature.pwa.WebAppUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.feature.top.sites.TopSite
//import mozilla.components.feature.toolbar.ToolbarFeature
import mozilla.components.lib.state.ext.flow
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.UserInteractionHandler
/* CENO: ifAnyChanged used for observing changes to extensions in addition to selectedTab*/
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import ie.equalit.cenoV2.addons.AddonsActivity
import ie.equalit.cenoV2.components.ceno.CenoWebExt.CENO_EXTENSION_ID
import ie.equalit.cenoV2.components.ceno.HttpsByDefaultWebExt.HTTPS_BY_DEFAULT_EXTENSION_ID
/* CENO: This components.ceno.toolbar replaces ToolbarFeature a-c commented out above */
import ie.equalit.cenoV2.components.ceno.toolbar.ToolbarFeature
import ie.equalit.cenoV2.components.ceno.UblockOriginWebExt.UBLOCK_ORIGIN_EXTENSION_ID
import ie.equalit.cenoV2.components.ceno.WebExtensionToolbarFeature
import ie.equalit.cenoV2.ext.components
import ie.equalit.cenoV2.ext.share
import ie.equalit.cenoV2.settings.SettingsActivity
//import ie.equalit.cenoV2.tabs.synced.SyncedTabsActivity

/* CENO: Add onTabUrlChange listener to control which fragment is displayed, Home or Browser */
@Suppress("LongParameterList")
class ToolbarIntegration(
    private val context: Context,
    toolbar: BrowserToolbar,
    historyStorage: HistoryStorage,
    store: BrowserStore,
    private val sessionUseCases: SessionUseCases,
    private val tabsUseCases: TabsUseCases,
    private val webAppUseCases: WebAppUseCases,
    sessionId: String? = null,
    private val onTabUrlChange: (String) -> Unit,
    isPrivate : Boolean
) : LifecycleAwareFeature, UserInteractionHandler {
    private val shippedDomainsProvider = ShippedDomainsProvider().also {
        it.initialize(context)
    }

    private val scope = MainScope()

    /* CENO: Create WebExtension Toolbar Feature to handle adding browserAction and pageAction buttons */
    private val cenoToolbarFeature = WebExtensionToolbarFeature(context, toolbar)

    private var isCurrentUrlPinned = false

    private fun menuToolbar(session: SessionState?): RowMenuCandidate {
        val tint = ContextCompat.getColor(context, R.color.icons)

        /* CENO: Hide forward and stop row menu items when they are not relevant */
        val rowMenuItems : MutableList<SmallMenuCandidate>  = emptyList<SmallMenuCandidate>().toMutableList()
        if (session?.content?.canGoForward == true) {
            rowMenuItems += SmallMenuCandidate(
                contentDescription = "Forward",
                icon = DrawableMenuIcon(
                    context,
                    mozilla.components.ui.icons.R.drawable.mozac_ic_forward,
                    tint = tint
                ),
                containerStyle = ContainerStyle(
                    isEnabled = session.content.canGoForward
                )
            ) {
                sessionUseCases.goForward.invoke()
            }
        }

        rowMenuItems += SmallMenuCandidate(
            contentDescription = "Refresh",
            icon = DrawableMenuIcon(
                context,
                mozilla.components.ui.icons.R.drawable.mozac_ic_refresh,
                tint = tint
            )
        ) {
            sessionUseCases.reload.invoke()
        }

        if (session?.content?.loading == true) {
            rowMenuItems += SmallMenuCandidate(
                contentDescription = "Stop",
                icon = DrawableMenuIcon(
                    context,
                    mozilla.components.ui.icons.R.drawable.mozac_ic_stop,
                    tint = tint
                )
            ) {
                sessionUseCases.stopLoading.invoke()
            }
        }

        return RowMenuCandidate(rowMenuItems)
    }

    private fun sessionMenuItems(sessionState: SessionState): List<MenuCandidate> {

        val sessionItems: MutableList<MenuCandidate> = emptyList<MenuCandidate>().toMutableList()
        sessionItems += menuToolbar(sessionState)

        sessionItems += listOf(TextMenuCandidate("Share") {
            val url = sessionState.content.url
            context.share(url)
        },

            CompoundMenuCandidate(
                text = "Request desktop site",
                isChecked = sessionState.content.desktopMode,
                end = CompoundMenuCandidate.ButtonType.SWITCH
            ) { checked ->
                sessionUseCases.requestDesktopSite.invoke(checked)
            })

        if (webAppUseCases.isPinningSupported()) {
            sessionItems += TextMenuCandidate(
                text = "Add to homescreen",
                containerStyle = ContainerStyle(
                    isVisible = webAppUseCases.isPinningSupported()
                )
            ) {
                scope.launch { webAppUseCases.addToHomescreen() }
            }
        } else {
            null
        }

        /* CENO: Add menu option for adding or removing a shortcut from the homepage */
        if (isCurrentUrlPinned) {
            sessionItems += TextMenuCandidate(
                text = "Remove from shortcuts",
            ) {
                scope.launch {
                    val removedTopSite: TopSite? =
                        context.components.core.cenoPinnedSiteStorage
                            .getPinnedSites()
                            .find { it.url == sessionState.content.url }
                    if (removedTopSite != null) {
                        with(context.components.useCases.cenoTopSitesUseCase) {
                            removeTopSites(removedTopSite)
                        }
                    }
                }
            }
        }
        else {
            sessionItems += TextMenuCandidate(
                text = "Add to shortcuts",
            ) {
                scope.launch {
                    //val context = swipeRefresh.context
                    val numPinnedSites = context.components.core.cenoTopSitesStorage.cachedTopSites
                        .filter { it is TopSite.Default || it is TopSite.Pinned }.size

                    if (numPinnedSites >= context.components.cenoPreferences.topSitesMaxLimit) {
                        AlertDialog.Builder(context).apply {
                            setTitle(R.string.shortcut_max_limit_title)
                            setMessage(R.string.shortcut_max_limit_content)
                            setPositiveButton(R.string.top_sites_max_limit_confirmation_button) { dialog, _ ->
                                dialog.dismiss()
                            }
                            create()
                        }.show()
                    } else {
                        sessionState.let {
                            with(context.components.useCases.cenoTopSitesUseCase) {
                                addPinnedSites(it.content.title, it.content.url)
                            }
                        }
                    }
                }
            }
        }
        sessionItems += TextMenuCandidate(
                text = "Find in Page"
            ) {
                FindInPageIntegration.launch?.invoke()
            }

        return sessionItems
    }

    private fun menuItems(sessionState: SessionState?): List<MenuCandidate> {
        val sessionMenuItems = if (sessionState != null) {
            sessionMenuItems(sessionState)
        } else {
            emptyList()
        }

        /* CENO: List of persistent menu items, will be joined with session/extension-dependent items */
        val staticMenuItems = listOf(
            TextMenuCandidate(text = "Add-ons") {
                val intent = Intent(context, AddonsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            },
            /*
            TextMenuCandidate(text = "Synced Tabs") {
                val intent = Intent(context, SyncedTabsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            },
             */
            TextMenuCandidate(text = "Report issue") {
                tabsUseCases.addTab(
                    url = "https://github.com/mozilla-mobile/reference-browser/issues/new"
                )
            },

            TextMenuCandidate(text = "Settings") {
                val intent = Intent(context, SettingsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        )

        /* CENO: Only add CENO menu items to list if their browserActions are not null */
        val cenoMenuItems : MutableList<MenuCandidate>  = emptyList<MenuCandidate>().toMutableList()
        cenoToolbarFeature.getBrowserAction(CENO_EXTENSION_ID)?.let{
            cenoMenuItems += TextMenuCandidate(
                text = "CENO",
                onClick = it
            )
        }

        cenoToolbarFeature.getBrowserAction(HTTPS_BY_DEFAULT_EXTENSION_ID)?.let{
            cenoMenuItems += TextMenuCandidate(
                text = "HTTPS by default",
                onClick = it
            )
        }

        cenoToolbarFeature.getBrowserAction(UBLOCK_ORIGIN_EXTENSION_ID)?.let{
            cenoMenuItems += TextMenuCandidate(
                text = "uBlock Origin",
                onClick = it
            )
        }
        return sessionMenuItems + staticMenuItems + cenoMenuItems
    }

    private val menuController: MenuController = BrowserMenuController()

    init {
        toolbar.display.indicators = listOf(
            DisplayToolbar.Indicators.SECURITY,
            DisplayToolbar.Indicators.TRACKING_PROTECTION
        )
        toolbar.display.displayIndicatorSeparator = true
        toolbar.display.menuController = menuController

        toolbar.display.hint = context.getString(R.string.toolbar_hint)
        toolbar.edit.hint = context.getString(R.string.toolbar_hint)

        ToolbarAutocompleteFeature(toolbar).apply {
            addHistoryStorageProvider(historyStorage)
            addDomainProvider(shippedDomainsProvider)
        }

        /* CENO: Set toolbar appearance based on whether current tab is private or not */
        toolbar.private = isPrivate
        if (isPrivate) {
            toolbar.display.setUrlBackground(
                ResourcesCompat.getDrawable(context.resources, R.drawable.url_private_background, context.theme)
            )
            toolbar.background = ResourcesCompat.getDrawable(context.resources, R.drawable.toolbar_background, context.theme)
        }
        else {
            toolbar.display.setUrlBackground(
                ResourcesCompat.getDrawable(context.resources, R.drawable.url_background, context.theme)
            )
        }


        /* CENO: launch coroutine to watch for changes to list of top sites
         * and update the isCurrentUrlPinned flag and resubmit */
        scope.launch {
            context.components.appStore.flow()
                .map { state -> state.topSites }
                .ifChanged()
                .collect(){ topSites ->
                    isCurrentUrlPinned = topSites
                        .find { it.url == store.state.selectedTab?.content?.url } != null
                    /* Resubmit menu items in case state of pinned sites changed */
                    menuController.submitList(menuItems(store.state.selectedTab))
                }
        }

        /* CENO: launch coroutine to observe for changes to the current tab URL
        *  will handle fragment transactions between homepage and browser */
        scope.launch {
            store.flow()
                .map { state -> state.selectedTab?.content?.url }
                .ifChanged()
                .collect { newUrl ->
                    isCurrentUrlPinned = context.components.core.cenoTopSitesStorage
                        .getTopSites(context.components.cenoPreferences.topSitesMaxLimit)
                        .find { it.url == newUrl } != null
                    if (newUrl != null) {
                        onTabUrlChange.invoke(newUrl)
                    }
                }
        }

        /* CENO: this coroutine must also monitor for changes to the extensions
         * so their browserActions get added to the three-dot menu once installed */
        scope.launch {
            store.flow()
                .ifAnyChanged { arrayOf(it.selectedTab, it.extensions) }
                .collect { state ->
                    menuController.submitList(menuItems(state.selectedTab))
                    /* pageAction buttons are removed globally,
                     * manually add only CENO pageAction button here
                     */
                    cenoToolbarFeature.addPageActionButton(CENO_EXTENSION_ID)
                }
        }
    }

    /* CENO: Requires components.ceno.toolbar which add hiddenAddressList
     * enabling specified urls to be hidden in the address bar */
    private val toolbarFeature: ToolbarFeature = ToolbarFeature(
        toolbar,
        context.components.core.store,
        context.components.useCases.sessionUseCases.loadUrl,
        { searchTerms ->
            context.components.useCases.searchUseCases.defaultSearch.invoke(
                searchTerms = searchTerms,
                searchEngine = null,
                parentSessionId = null
            )
        },
        sessionId,
        hiddenAddressList = listOf(CenoHomeFragment.ABOUT_HOME)
    )

    override fun start() {
        toolbarFeature.start()
    }

    override fun stop() {
        toolbarFeature.stop()
    }

    override fun onBackPressed(): Boolean {
        return toolbarFeature.onBackPressed()
    }
}
