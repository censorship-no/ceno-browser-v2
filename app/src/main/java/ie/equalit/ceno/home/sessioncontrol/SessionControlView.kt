package ie.equalit.ceno.home.sessioncontrol

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ie.equalit.ceno.R
import ie.equalit.ceno.browser.BrowsingMode
import mozilla.components.feature.top.sites.TopSite
import ie.equalit.ceno.components.ceno.appstate.AppState
import ie.equalit.ceno.ext.cenoPreferences
import ie.equalit.ceno.home.CenoMessageCard
import ie.equalit.ceno.home.HomeCardSwipeCallback
import ie.equalit.ceno.home.RssAnnouncementResponse
import ie.equalit.ceno.utils.CenoPreferences

// This method got a little complex with the addition of the tab tray feature flag
// When we remove the tabs from the home screen this will get much simpler again.
@Suppress("ComplexMethod", "LongParameterList")
@VisibleForTesting
internal fun normalModeAdapterItems(
    settings: CenoPreferences,
    topSites: List<TopSite>,
    messageCard: CenoMessageCard,
    announcement: RssAnnouncementResponse
): List<AdapterItem> {
    val items = mutableListOf<AdapterItem>()
    var shouldShowCustomizeHome = false

    // Add a synchronous, unconditional and invisible placeholder so home is anchored to the top when created.
    items.add(AdapterItem.TopPlaceholderItem)

    if (settings.showThanksCard) {
        items.add(AdapterItem.CenoMessageItem(messageCard))
    }
    if (settings.showCenoModeItem) {
        items.add(AdapterItem.CenoModeItem)
    }

    items.add(AdapterItem.CenoAnnouncementItem(announcement))

    if (/*settings.showTopSitesFeature && */ topSites.isNotEmpty()) {
        items.add(AdapterItem.TopSitePager(topSites))
    }
    return items
}

internal fun personalModeAdapterItems(): List<AdapterItem> = listOf(AdapterItem.PersonalModeDescriptionItem)

private fun AppState.toAdapterList(prefs: CenoPreferences, messageCard: CenoMessageCard, announcement: RssAnnouncementResponse): List<AdapterItem> = when (mode) {
    BrowsingMode.Normal ->
        normalModeAdapterItems(
            prefs,
            topSites,
            messageCard,
            announcement
        )
    BrowsingMode.Personal -> personalModeAdapterItems()
}


class SessionControlView(
    val containerView: View,
    viewLifecycleOwner: LifecycleOwner,
    internal val interactor: SessionControlInteractor
) {
    val view: RecyclerView = containerView as RecyclerView

    private val sessionControlAdapter = SessionControlAdapter(
        interactor,
        viewLifecycleOwner
    )

    init {
        view.apply {
            adapter = sessionControlAdapter
            layoutManager = object : LinearLayoutManager(containerView.context) {
                override fun onLayoutCompleted(state: RecyclerView.State?) {
                    super.onLayoutCompleted(state)
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(HomeCardSwipeCallback(
            swipeDirs = ItemTouchHelper.LEFT,
            dragDirs = 0,
            interactor = interactor
        ))
        itemTouchHelper.attachToRecyclerView(view)
    }

    fun update(state: AppState, announcement: RssAnnouncementResponse) {
        /* TODO: add onboarding pages
        if (state.shouldShowHomeOnboardingDialog(view.context.settings())) {
            interactor.showOnboardingDialog()
        }
         */

        val messageCard = CenoMessageCard(
            text = view.context.getString(R.string.onboarding_thanks_text),
            title = view.context.getString(R.string.onboarding_thanks_title)
        )
        sessionControlAdapter.submitList(state.toAdapterList(view.context.cenoPreferences(), messageCard, announcement))

    }
}