package ie.equalit.ceno.tabs

import androidx.lifecycle.LifecycleOwner
import ie.equalit.ceno.browser.BrowsingModeManager
import ie.equalit.ceno.ui.theme.ThemeManager
import mozilla.components.browser.state.selector.findCustomTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.ui.tabcounter.TabCounterMenu

class TabCounterView (
    toolbar: Toolbar,
    store: BrowserStore,
    sessionId: String? = null,
    lifecycleOwner: LifecycleOwner,
    showTabs: () -> Unit,
    tabCounterMenu: TabCounterMenu? = null,
    countBasedOnSelectedTabType: Boolean = true,
    browsingModeManager: BrowsingModeManager,
    themeManager: ThemeManager
){
    init {
        run {
            // this feature is not used for Custom Tabs
            if (sessionId != null && store.state.findCustomTab(sessionId) != null) return@run

            val tabsAction = TabCounterToolbarButton(
                store = store,
                lifecycleOwner = lifecycleOwner,
                showTabs = showTabs,
                menu = tabCounterMenu,
                countBasedOnSelectedTabType = countBasedOnSelectedTabType,
                browsingModeManager = browsingModeManager,
                themeManager = themeManager
            )
            toolbar.addBrowserAction(tabsAction)
        }
    }
}