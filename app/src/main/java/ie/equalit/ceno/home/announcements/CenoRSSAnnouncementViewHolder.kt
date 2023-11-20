package ie.equalit.ceno.home.announcements

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.view.isGone
import ie.equalit.ceno.R
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

    fun bind(response: RssAnnouncementResponse) {

        binding.rssTitle.setOnClickListener {
            val listIsHidden = binding.rssAnnouncementsRecyclerView.visibility == View.GONE

            binding.rssAnnouncementsRecyclerView.isGone = !listIsHidden
            binding.rssTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (listIsHidden) R.drawable.ic_announcement_expanded else R.drawable.ic_announcement_collapsed,
                0,
                if (listIsHidden) R.drawable.ic_arrow_expanded else R.drawable.ic_arrow_collapsed,
                0
            )
        }

        binding.rssAnnouncementsRecyclerView.adapter = RssAnnouncementSubAdapter(interactor).apply {
            submitList(response.items)
        }

    }

    private fun Drawable.tinted(@ColorInt tintColor: Int? = null, tintMode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN) =
        apply {
            setTintList(tintColor?.toColorStateList())
            setTintMode(tintMode)
        }

    private fun Int.toColorStateList() = ColorStateList.valueOf(this)

    companion object {
        val homepageCardType = HomepageCardType.ANNOUNCEMENTS_CARD
    }
}