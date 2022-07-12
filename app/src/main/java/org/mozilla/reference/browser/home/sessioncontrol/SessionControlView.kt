package org.mozilla.reference.browser.home.sessioncontrol

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.feature.top.sites.TopSite
import org.mozilla.reference.browser.ext.cenoPreferences
import org.mozilla.reference.browser.utils.CenoPreferences

// This method got a little complex with the addition of the tab tray feature flag
// When we remove the tabs from the home screen this will get much simpler again.
@Suppress("ComplexMethod", "LongParameterList")
@VisibleForTesting
internal fun normalModeAdapterItems(
    settings: CenoPreferences,
    topSites: List<TopSite>,
): List<AdapterItem> {
    val items = mutableListOf<AdapterItem>()
    var shouldShowCustomizeHome = false

    if (/*settings.showTopSitesFeature && */ topSites.isNotEmpty()) {
        items.add(AdapterItem.TopSitePager(topSites))
    }
    return items
}

class SessionControlView(
    val containerView: View,
    viewLifecycleOwner: LifecycleOwner,
    internal val interactor: SessionControlInteractor,
    val data: List<TopSite>
) {
    val view: RecyclerView = containerView as RecyclerView

    private val sessionControlAdapter = SessionControlAdapter(
        interactor,
        viewLifecycleOwner,
        containerView.context,
        normalModeAdapterItems(
            view.context.cenoPreferences(),
            data
        )
    )

    init {
        view.apply {
            adapter = sessionControlAdapter
            layoutManager = object : LinearLayoutManager(containerView.context) {
                override fun onLayoutCompleted(state: RecyclerView.State?) {
                    super.onLayoutCompleted(state)

                    //JumpBackInCFRDialog(view).showIfNeeded()
                }
            }
        }
    }
}