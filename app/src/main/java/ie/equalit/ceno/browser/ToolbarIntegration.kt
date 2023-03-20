/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.browser

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import ie.equalit.ceno.R
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
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.feature.pwa.WebAppUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.feature.toolbar.ToolbarFeature
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.lib.state.ext.flow
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.UserInteractionHandler
/* CENO: ifAnyChanged used for observing changes to extensions in addition to selectedTab*/
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import ie.equalit.ceno.addons.AddonsActivity
import ie.equalit.ceno.components.ceno.CenoWebExt.CENO_EXTENSION_ID
import ie.equalit.ceno.components.ceno.ClearButtonFeature
import ie.equalit.ceno.components.ceno.HttpsByDefaultWebExt.HTTPS_BY_DEFAULT_EXTENSION_ID
import ie.equalit.ceno.components.ceno.UblockOriginWebExt.UBLOCK_ORIGIN_EXTENSION_ID
import ie.equalit.ceno.components.ceno.WebExtensionToolbarFeature
import ie.equalit.ceno.ext.components
import ie.equalit.ceno.ext.getPreferenceKey
import ie.equalit.ceno.settings.CenoSettings
import ie.equalit.ceno.settings.SettingsFragment

/* CENO: Add onTabUrlChange listener to control which fragment is displayed, Home or Browser */
@Suppress("LongParameterList")
class ToolbarIntegration(
    private val context: Context,
    private val activity: FragmentActivity,
    toolbar: BrowserToolbar,
    historyStorage: PlacesHistoryStorage,
    store: BrowserStore,
    private val sessionUseCases: SessionUseCases,
    private val tabsUseCases: TabsUseCases,
    private val webAppUseCases: WebAppUseCases,
    sessionId: String? = null,
    private val onTabUrlChange: (String) -> Unit,
) : LifecycleAwareFeature, UserInteractionHandler {
    private val shippedDomainsProvider = ShippedDomainsProvider().also {
        it.initialize(context)
    }

    private val scope = MainScope()

    /* CENO: Create WebExtension Toolbar Feature to handle adding browserAction and pageAction buttons */
    private val cenoToolbarFeature = WebExtensionToolbarFeature(context, toolbar)

    private var isCurrentUrlPinned = false

    private fun menuToolbar(session: SessionState?): RowMenuCandidate {
        val tintEnabled = ContextCompat.getColor(context, R.color.fx_mobile_icon_color_primary)
        val tintDisabled = ContextCompat.getColor(context, R.color.fx_mobile_icon_color_primary_inactive)
        val canGoBack = session?.content?.canGoBack!!
        val canGoForward = session.content.canGoForward

        /* CENO: Disable forward and stop row menu items when they are not relevant */
        val rowMenuItems : MutableList<SmallMenuCandidate>  = emptyList<SmallMenuCandidate>().toMutableList()
        rowMenuItems += SmallMenuCandidate(
            contentDescription = context.getString(R.string.browser_menu_back),
            icon = DrawableMenuIcon(
                context,
                R.drawable.mozac_ic_back,
                tint = if (canGoBack) tintEnabled else tintDisabled
            ),
            containerStyle = ContainerStyle(
                isEnabled = canGoBack
            )
        ) {
            if (canGoBack) {
                sessionUseCases.goBack.invoke()
            }
        }

        rowMenuItems += SmallMenuCandidate(
            contentDescription = context.getString(R.string.browser_menu_forward),
            icon = DrawableMenuIcon(
                context,
                R.drawable.mozac_ic_forward,
                tint = if (canGoForward) tintEnabled else tintDisabled
            ),
            containerStyle = ContainerStyle(
                isEnabled = canGoForward
            )
        ) {
            if (canGoForward) {
                sessionUseCases.goForward.invoke()
            }
        }

        /* CENO: Switch from cancel to refresh option when loading is finished */
        if (session.content.loading) {
            rowMenuItems += SmallMenuCandidate(
                contentDescription = context.getString(R.string.browser_menu_stop),
                icon = DrawableMenuIcon(
                    context,
                    R.drawable.mozac_ic_stop,
                )
            ) {
                sessionUseCases.stopLoading.invoke()
            }
        }
        else {
            rowMenuItems += SmallMenuCandidate(
                contentDescription = context.getString(R.string.browser_menu_refresh),
                icon = DrawableMenuIcon(
                    context,
                    R.drawable.mozac_ic_refresh,
                )
            ) {
                sessionUseCases.reload.invoke()
            }
        }

        return RowMenuCandidate(rowMenuItems)
    }

    /* CENO: Add menu option for adding or removing a shortcut from the homepage */
    private fun shortcutMenuItem(sessionState: SessionState): MenuCandidate {
        return if (isCurrentUrlPinned) {
            TextMenuCandidate(
                text = context.getString(R.string.browser_menu_remove_from_shortcuts),
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
            TextMenuCandidate(
                text = context.getString(R.string.browser_menu_add_to_shortcuts),
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
    }

    private fun menuItems(sessionState: SessionState?): List<MenuCandidate> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val menuItemsList: MutableList<MenuCandidate> = emptyList<MenuCandidate>().toMutableList()
        if (sessionState != null) {
            menuItemsList += menuToolbar(sessionState)
            menuItemsList += CompoundMenuCandidate(
                text = context.getString(R.string.browser_menu_desktop_site),
                isChecked = sessionState.content.desktopMode,
                end = CompoundMenuCandidate.ButtonType.SWITCH
            ) { checked ->
                sessionUseCases.requestDesktopSite.invoke(checked)
            }
        }

        val clearButtonFeature = ClearButtonFeature(
            context,
            prefs.getString(
                context.getPreferenceKey(R.string.pref_key_clear_behavior), "0")!!
                .toInt()
        )

        if (prefs.getBoolean(context.getPreferenceKey(R.string.pref_key_clear_in_menu), true)) {
            menuItemsList += TextMenuCandidate(
                text = context.getString(R.string.ceno_clear_dialog_title),
                onClick = {
                    clearButtonFeature.onClick()
                }
            )
        }
        if (sessionState != null) {
            if (webAppUseCases.isPinningSupported()) {
                menuItemsList += TextMenuCandidate(
                    text = context.getString(R.string.browser_menu_add_to_homescreen),
                    containerStyle = ContainerStyle(
                        isVisible = webAppUseCases.isPinningSupported()
                    )
                ) {
                    scope.launch { webAppUseCases.addToHomescreen() }
                }
            }
            menuItemsList += shortcutMenuItem(sessionState)

            menuItemsList += TextMenuCandidate(
                text = context.getString(R.string.browser_menu_find_in_page)
            ) {
                FindInPageIntegration.launch?.invoke()
            }
        }

        /* CENO: Only add extension menu items to list if their browserActions are not null */
        cenoToolbarFeature.getBrowserAction(HTTPS_BY_DEFAULT_EXTENSION_ID)?.let{
            menuItemsList += TextMenuCandidate(
                text = context.getString(R.string.browser_menu_https_by_default),
                onClick = it
            )
        }

        cenoToolbarFeature.getBrowserAction(UBLOCK_ORIGIN_EXTENSION_ID)?.let{
            menuItemsList += TextMenuCandidate(
                text = context.getString(R.string.browser_menu_ublock_origin),
                onClick = it
            )
        }

        menuItemsList += TextMenuCandidate(text = context.getString(R.string.browser_menu_add_ons)) {
            val intent = Intent(context, AddonsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        if (sessionState != null) {
            menuItemsList += TextMenuCandidate(text = context.getString(R.string.browser_menu_settings)) {
                /* CENO: Switch to SettingsFragment instead of starting a new activity */
                CenoSettings.setStatusUpdateRequired(context, true)
                activity.supportFragmentManager.beginTransaction().apply {
                    replace(
                        R.id.container,
                        SettingsFragment.create(sessionState.id),
                        SettingsFragment.TAG
                    )
                    addToBackStack(null)
                    commit()
                }
            }
        }

        return menuItemsList
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
            updateAutocompleteProviders(
                listOf(historyStorage, shippedDomainsProvider),
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
                    toolbar.display.setOnTrackingProtectionClickedListener {
                        cenoToolbarFeature.getPageAction(CENO_EXTENSION_ID)?.invoke()
                    }
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
