package ie.equalit.cenoV2.home.sessioncontrol

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.feature.top.sites.TopSite
import ie.equalit.cenoV2.home.CenoModeViewHolder
import ie.equalit.cenoV2.home.TopPlaceholderViewHolder
import ie.equalit.cenoV2.home.topsites.TopSitePagerViewHolder

sealed class AdapterItem(@LayoutRes val viewType: Int) {

    object TopPlaceholderItem : AdapterItem(TopPlaceholderViewHolder.LAYOUT_ID)

    object CenoModeItem : AdapterItem(CenoModeViewHolder.LAYOUT_ID)

    /**
     * Contains a set of [Pair]s where [Pair.first] is the index of the changed [TopSite] and
     * [Pair.second] is the new [TopSite].
     */
    data class TopSitePagerPayload(
        val changed: Set<Pair<Int, TopSite>>
    )

    data class TopSitePager(val topSites: List<TopSite>) :
        AdapterItem(TopSitePagerViewHolder.LAYOUT_ID) {
        override fun sameAs(other: AdapterItem): Boolean {
            return other is TopSitePager
        }

        override fun contentsSameAs(other: AdapterItem): Boolean {
            val newTopSites = (other as? TopSitePager) ?: return false
            if (newTopSites.topSites.size != this.topSites.size) return false
            val newSitesSequence = newTopSites.topSites.asSequence()
            val oldTopSites = this.topSites.asSequence()
            return newSitesSequence.zip(oldTopSites).all { (new, old) -> new == old }
        }

        /**
         * Returns a payload if there's been a change, or null if not, but adds a "dummy" item for
         * each deleted [TopSite]. This is done in order to more easily identify the actual views
         * that need to be removed in [TopSitesPagerAdapter.update].
         *
         * See https://github.com/mozilla-mobile/fenix/pull/20189#issuecomment-877124730
         */
        @Suppress("ComplexCondition")
        override fun getChangePayload(newItem: AdapterItem): Any? {
            val newTopSites = (newItem as? TopSitePager)
            val oldTopSites = (this as? TopSitePager)

            if (newTopSites == null || oldTopSites == null ||
                newTopSites.topSites.size > oldTopSites.topSites.size ||
                (newTopSites.topSites.size > TopSitePagerViewHolder.TOP_SITES_PER_PAGE)
                != (oldTopSites.topSites.size > TopSitePagerViewHolder.TOP_SITES_PER_PAGE)
            ) {
                return null
            }

            val changed = mutableSetOf<Pair<Int, TopSite>>()

            for ((index, item) in oldTopSites.topSites.withIndex()) {
                val changedItem =
                    newTopSites.topSites.getOrNull(index) ?: TopSite.Frecent(-1, "REMOVED", "", 0)
                if (changedItem != item) {
                    changed.add((Pair(index, changedItem)))
                }
            }
            return if (changed.isNotEmpty()) TopSitePagerPayload(changed) else null
        }
    }

    /**
     * True if this item represents the same value as other. Used by [AdapterItemDiffCallback].
     */
    open fun sameAs(other: AdapterItem) = this::class == other::class

    /**
     * Returns a payload if there's been a change, or null if not
     */
    open fun getChangePayload(newItem: AdapterItem): Any? = null

    open fun contentsSameAs(other: AdapterItem) = this::class == other::class
}
class AdapterItemDiffCallback : DiffUtil.ItemCallback<AdapterItem>() {
    override fun areItemsTheSame(oldItem: AdapterItem, newItem: AdapterItem) =
        oldItem.sameAs(newItem)

    @Suppress("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: AdapterItem, newItem: AdapterItem) =
        oldItem.contentsSameAs(newItem)

    override fun getChangePayload(oldItem: AdapterItem, newItem: AdapterItem): Any? {
        return oldItem.getChangePayload(newItem) ?: return super.getChangePayload(oldItem, newItem)
    }
}

class SessionControlAdapter internal constructor(
    private val interactor: SessionControlInteractor,
    private val viewLifecycleOwner: LifecycleOwner
    ) :
    ListAdapter<AdapterItem, RecyclerView.ViewHolder>(AdapterItemDiffCallback())
    {
    // inflates the row layout from xml when needed
    // This method triggers the ComplexMethod lint error when in fact it's quite simple.
    @SuppressWarnings("ComplexMethod", "LongMethod", "ReturnCount")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            TopPlaceholderViewHolder.LAYOUT_ID -> TopPlaceholderViewHolder(view)
            CenoModeViewHolder.LAYOUT_ID -> CenoModeViewHolder(view, interactor)
            TopSitePagerViewHolder.LAYOUT_ID -> TopSitePagerViewHolder(
                view = view,
                viewLifecycleOwner = viewLifecycleOwner,
                interactor = interactor
            )
            /*
            OnboardingHeaderViewHolder.LAYOUT_ID -> OnboardingHeaderViewHolder(view)
            OnboardingSectionHeaderViewHolder.LAYOUT_ID -> OnboardingSectionHeaderViewHolder(view)
            OnboardingManualSignInViewHolder.LAYOUT_ID -> OnboardingManualSignInViewHolder(view)
            OnboardingThemePickerViewHolder.LAYOUT_ID -> OnboardingThemePickerViewHolder(view)
            OnboardingTrackingProtectionViewHolder.LAYOUT_ID -> OnboardingTrackingProtectionViewHolder(
                view
            )
            OnboardingPrivacyNoticeViewHolder.LAYOUT_ID -> OnboardingPrivacyNoticeViewHolder(
                view,
                interactor
            )
            OnboardingFinishViewHolder.LAYOUT_ID -> OnboardingFinishViewHolder(view, interactor)
            OnboardingToolbarPositionPickerViewHolder.LAYOUT_ID -> OnboardingToolbarPositionPickerViewHolder(
                view
            )
            BottomSpacerViewHolder.LAYOUT_ID -> BottomSpacerViewHolder(view)
             */
            else -> throw IllegalStateException()
        }
    }

    override fun getItemViewType(position: Int) = getItem(position).viewType

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNullOrEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            when (holder) {
                is TopSitePagerViewHolder -> {
                    if (payloads[0] is AdapterItem.TopSitePagerPayload) {
                        val payload = payloads[0] as AdapterItem.TopSitePagerPayload
                        holder.update(payload)
                    }
                }
            }
        }
    }

    @SuppressWarnings("ComplexMethod")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is TopPlaceholderViewHolder -> {
                holder.bind()
            }
            is CenoModeViewHolder -> {
                holder.bind()
            }
            is TopSitePagerViewHolder -> {
                holder.bind((item as AdapterItem.TopSitePager).topSites)
            }
            /*
            is OnboardingSectionHeaderViewHolder -> holder.bind(
                (item as AdapterItem.OnboardingSectionHeader).labelBuilder
            )
            is OnboardingManualSignInViewHolder -> holder.bind()
            */
        }
    }
}