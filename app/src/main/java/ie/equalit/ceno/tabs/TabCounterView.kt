package ie.equalit.ceno.tabs

import androidx.lifecycle.LifecycleOwner
import ie.equalit.ceno.browser.BrowsingModeManager
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
    browsingModeManager: BrowsingModeManager
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
                browsingModeManager = browsingModeManager
            )
            toolbar.addBrowserAction(tabsAction)
        }
    }
}