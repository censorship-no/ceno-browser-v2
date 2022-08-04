/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser.tabs

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.DefaultTabViewHolder
import mozilla.components.browser.tabstray.TabsAdapter
import mozilla.components.browser.tabstray.TabsTray
import mozilla.components.browser.tabstray.TabsTrayStyling
import mozilla.components.browser.tabstray.ViewHolderProvider
import mozilla.components.browser.thumbnails.loader.ThumbnailLoader
import mozilla.components.feature.tabs.tabstray.TabsFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.reference.browser.R
import org.mozilla.reference.browser.browser.BrowserFragment
import org.mozilla.reference.browser.browser.CenoHomeFragment
import org.mozilla.reference.browser.ext.components
import org.mozilla.reference.browser.ext.requireComponents

/**
 * A fragment for displaying the tabs tray.
 */
class TabsTrayFragment : Fragment(), UserInteractionHandler {
    private var tabsFeature: TabsFeature? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_tabstray, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val trayAdapter = createAndSetupTabsTray(requireContext())

        tabsFeature = TabsFeature(
            trayAdapter,
            requireComponents.core.store,
            { closeTabsTray(toHome = true, withNewTab = true) }
        ) {
            /* CENO: check if current tab is normal/private, set tabs panel and filter to match */
            if(requireComponents.core.store.state.selectedTab?.content?.private == true) {
                selectTabInPanel(isPrivate = true)
                it.content.private
            } else {
                selectTabInPanel(isPrivate = false)
                !it.content.private
            }
        }

        val tabsPanel: TabsPanel = view.findViewById(R.id.tabsPanel)
        val tabsToolbar: TabsToolbar = view.findViewById(R.id.tabsToolbar)

        tabsPanel.initialize(tabsFeature, updateTabsToolbar = ::updateTabsToolbar)
        tabsToolbar.initialize(tabsFeature, ::closeTabsTray)
    }

    override fun onStart() {
        super.onStart()

        tabsFeature?.start()
    }

    override fun onStop() {
        super.onStop()

        tabsFeature?.stop()
    }

    override fun onBackPressed(): Boolean {
        closeTabsTray()
        return true
    }

    /* CENO: Modify closeTabsTray function to take booleans for determining
     * how to close the TabsTrayFragment, i.e. to open the Home or Browser Fragment,
     * with or without a new blank tab? */
    private fun closeTabsTray(toHome: Boolean = false, withNewTab: Boolean = false) {
        if(toHome) {
            activity?.supportFragmentManager?.beginTransaction()?.apply {
                replace(R.id.container, CenoHomeFragment.create(), CenoHomeFragment.TAG)
                commit()
            }
            if (withNewTab)
                requireComponents.useCases.tabsUseCases.addTab(CenoHomeFragment.ABOUT_HOME)
        }
        else {
            activity?.supportFragmentManager?.beginTransaction()?.apply {
                replace(R.id.container, BrowserFragment.create(), BrowserFragment.TAG)
                commit()
            }
        }
    }

    private fun updateTabsToolbar(isPrivate: Boolean) {
        val tabsToolbar = requireView().findViewById<TabsToolbar>(R.id.tabsToolbar)
        tabsToolbar.updateToolbar(isPrivate)
    }

    /* CENO: Needed method to select normal/private tab during init of TabsTray */
    private fun selectTabInPanel(isPrivate: Boolean) {
        val tabsPanel = requireView().findViewById<TabsPanel>(R.id.tabsPanel)
        tabsPanel.selectTab(isPrivate)
    }

    private fun createAndSetupTabsTray(context: Context): TabsTray {
        val layoutManager = LinearLayoutManager(context)
        val thumbnailLoader = ThumbnailLoader(context.components.core.thumbnailStorage)
        val trayStyling = TabsTrayStyling(itemBackgroundColor = Color.TRANSPARENT, itemTextColor = Color.WHITE)
        val viewHolderProvider: ViewHolderProvider = { viewGroup ->
            val view = LayoutInflater.from(context)
                .inflate(R.layout.browser_tabstray_item, viewGroup, false)

            DefaultTabViewHolder(view, thumbnailLoader)
        }
        val tabsAdapter = TabsAdapter(
            thumbnailLoader = thumbnailLoader,
            viewHolderProvider = viewHolderProvider,
            styling = trayStyling,
            delegate = object : TabsTray.Delegate {
                override fun onTabSelected(tab: TabSessionState, source: String?) {
                    requireComponents.useCases.tabsUseCases.selectTab(tab.id)
                    /* CENO: Check if tab should open HomeFragment or BrowserFragment after closing */
                    if(tab.content.url == CenoHomeFragment.ABOUT_HOME){
                        /* A home page "tab" was selected, close the tab tray and open the HomeFragment,
                         * but do not create a new tab to associate with the Fragment, since selectedTab is homepage */
                        closeTabsTray(toHome = true, withNewTab = false)
                    }else{
                        closeTabsTray()
                    }
                }

                override fun onTabClosed(tab: TabSessionState, source: String?) {
                    requireComponents.useCases.tabsUseCases.removeTab(tab.id)
                }
            }
        )

        val tabsTray = requireView().findViewById<RecyclerView>(R.id.tabsTray)
        tabsTray.layoutManager = layoutManager
        tabsTray.adapter = tabsAdapter

        TabsTouchHelper {
            requireComponents.useCases.tabsUseCases.removeTab(it.id)
        }.attachToRecyclerView(tabsTray)

        return tabsAdapter
    }

    companion object {
        /* CENO: Add a tag to keep track of whether this fragment is open */
        const val TAG = "TABS_TRAY"
    }
}
