package ie.equalit.ceno.home.announcements

import android.view.ContextThemeWrapper
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isGone
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ie.equalit.ceno.R
import ie.equalit.ceno.browser.BrowsingMode
import ie.equalit.ceno.databinding.RssAnnouncementItemBinding
import ie.equalit.ceno.home.BaseHomeCardViewHolder
import ie.equalit.ceno.home.HomepageCardType
import ie.equalit.ceno.home.RssAnnouncementResponse
import ie.equalit.ceno.home.sessioncontrol.HomePageInteractor

class CenoRSSAnnouncementViewHolder(
    itemView: View,
    interactor: HomePageInteractor
) : BaseHomeCardViewHolder(itemView, interactor) {

    private val binding = RssAnnouncementItemBinding.bind(itemView)
    private var mode: BrowsingMode = BrowsingMode.Normal
        get() {
            return field
        }
        set(value) {
            field = value
            update()
        }

    fun update() {
        val listIsHidden = binding.rssAnnouncementsRecyclerView.visibility == View.GONE
        val personalContext = ContextThemeWrapper(itemView.context, R.style.PersonalTheme)
        if (mode.isPersonal) {
            binding.rssTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.ceno_orange_200))
            DrawableCompat.setTint(binding.rssTitle.background, ContextCompat.getColor(itemView.context, R.color.ceno_orange_800))
            binding.rssAnnouncementsCard.background.setTint(ContextCompat.getColor(itemView.context, R.color.ceno_orange_900))
            binding.rssTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (listIsHidden) ContextCompat.getDrawable(personalContext, R.drawable.ic_announcement_collapsed) else ContextCompat.getDrawable(personalContext, R.drawable.ic_announcement_expanded),
                null,
                if (listIsHidden) ContextCompat.getDrawable(personalContext, R.drawable.ic_arrow_collapsed) else ContextCompat.getDrawable(personalContext, R.drawable.ic_arrow_expanded),
                null
            )

        } else {
            binding.rssTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.ceno_home_card_announcement_title_color))
            DrawableCompat.setTint(binding.rssTitle.background, ContextCompat.getColor(itemView.context, R.color.ceno_home_card_announcement_title_background))
            binding.rssAnnouncementsCard.background.setTint(ContextCompat.getColor(itemView.context, R.color.home_card_announcements_background))
            binding.rssTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (listIsHidden) R.drawable.ic_announcement_collapsed else R.drawable.ic_announcement_expanded,
                0,
                if (listIsHidden) R.drawable.ic_arrow_collapsed else R.drawable.ic_arrow_expanded,
                0
            )
        }
    }

    fun bind(response: RssAnnouncementResponse, mode: BrowsingMode) {
        this@CenoRSSAnnouncementViewHolder.mode = mode

        binding.rssTitle.text = itemView.context.getString(R.string.announcement_header, response.title, response.items.size)

        val personalContext = ContextThemeWrapper(itemView.context, R.style.PersonalTheme)

        binding.rssTitle.setOnClickListener {

            val listIsHidden = binding.rssAnnouncementsRecyclerView.visibility == View.GONE

            binding.rssAnnouncementsRecyclerView.isGone = !listIsHidden
            if (mode.isPersonal) {
                binding.rssTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    if (listIsHidden) ContextCompat.getDrawable(personalContext, R.drawable.ic_announcement_expanded) else ContextCompat.getDrawable(personalContext, R.drawable.ic_announcement_collapsed),
                    null,
                    if (listIsHidden) ContextCompat.getDrawable(personalContext, R.drawable.ic_arrow_expanded) else ContextCompat.getDrawable(personalContext, R.drawable.ic_arrow_collapsed),
                    null
                )
            } else {
                binding.rssTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    if (listIsHidden) R.drawable.ic_announcement_expanded else R.drawable.ic_announcement_collapsed,
                    0,
                    if (listIsHidden) R.drawable.ic_arrow_expanded else R.drawable.ic_arrow_collapsed,
                    0
                )
            }
        }

        binding.rssAnnouncementsRecyclerView.apply {
            adapter = RssAnnouncementSubAdapter(interactor, mode).apply {
                submitList(response.items)
            }
            layoutManager = object : LinearLayoutManager(binding.root.context) {
                override fun onLayoutCompleted(state: RecyclerView.State?) {
                    super.onLayoutCompleted(state)
                }
            }
        }

        ItemTouchHelper(
            AnnouncementCardSwipeCallback(
                swipeDirs = ItemTouchHelper.LEFT,
                dragDirs = 0,
                interactor = interactor
            )
        ).attachToRecyclerView(binding.rssAnnouncementsRecyclerView)
    }

    companion object {
        val homepageCardType = HomepageCardType.ANNOUNCEMENTS_CARD
    }
}