package ie.equalit.ceno.home.sessioncontrol

import android.annotation.SuppressLint
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
    mode: BrowsingMode,
    announcement: RssAnnouncementResponse?
): List<AdapterItem> {
    val items = mutableListOf<AdapterItem>()
    var shouldShowCustomizeHome = false

    // Add a synchronous, unconditional and invisible placeholder so home is anchored to the top when created.
    items.add(AdapterItem.TopPlaceholderItem)

    // Show announcements at the top
    announcement?.let { items.add(AdapterItem.CenoAnnouncementItem(it, BrowsingMode.Normal)) }

    items.add(AdapterItem.CenoModeItem(mode))

    items.add(AdapterItem.CenoMessageItem(messageCard))


    if (/*settings.showTopSitesFeature && */ topSites.isNotEmpty()) {
        items.add(AdapterItem.TopSitePager(topSites))
    }
    return items
}

internal fun personalModeAdapterItems(mode: BrowsingMode, announcement: RssAnnouncementResponse?): List<AdapterItem> {
    val items = mutableListOf<AdapterItem>()
    // Add a synchronous, unconditional and invisible placeholder so home is anchored to the top when created.
    items.add(AdapterItem.TopPlaceholderItem)
    // Show announcements at the top
    announcement?.let { items.add(AdapterItem.CenoAnnouncementItem(it, BrowsingMode.Personal)) }

    items.add(AdapterItem.CenoModeItem(mode))
    items.add(AdapterItem.PersonalModeDescriptionItem)

    return items
}
private fun AppState.toAdapterList(prefs: CenoPreferences, messageCard: CenoMessageCard, announcement: RssAnnouncementResponse?): List<AdapterItem> = when (mode) {
    BrowsingMode.Normal ->
        normalModeAdapterItems(
            prefs,
            topSites,
            messageCard,
            mode,
            announcement
        )
    BrowsingMode.Personal -> personalModeAdapterItems(mode, announcement)
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
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update(state: AppState, announcement: RssAnnouncementResponse?) {
        /* TODO: add onboarding pages
        if (state.shouldShowHomeOnboardingDialog(view.context.settings())) {
            interactor.showOnboardingDialog()
        }
         */

        val messageCard = CenoMessageCard(
            text = "Make the Ceno network stonger by becoming a bridge. You can enable Bridge Mode by going to Settings -> Enable Bridge Mode",
            title = "Enable Bridge Mode"
        )
        sessionControlAdapter.submitList(state.toAdapterList(view.context.cenoPreferences(), messageCard, announcement))
        sessionControlAdapter.notifyDataSetChanged()

    }
}