package ie.equalit.ceno.home


import android.view.View
import ie.equalit.ceno.databinding.RssAnnoucementsItemBinding
import ie.equalit.ceno.home.sessioncontrol.HomePageInteractor

class CenoRSSAnnouncementViewHolder(
    itemView: View,
    interactor: HomePageInteractor
) : BaseHomeCardViewHolder(itemView, interactor) {

    private val binding = RssAnnoucementsItemBinding.bind(itemView)

    fun bind(response: RssAnnouncementResponse) {
        binding.rssTitle.text = response.title
        binding.itemDate.text = response.item?.pubDate
        binding.tvMessage.text = response.item?.description
    }

    companion object {
        val homepageCardType = HomepageCardType.ANNOUNCEMENTS_CARD
    }
}