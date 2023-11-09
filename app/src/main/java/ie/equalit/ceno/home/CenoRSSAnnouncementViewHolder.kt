package ie.equalit.ceno.home


import android.view.View
import androidx.core.text.HtmlCompat
import ie.equalit.ceno.databinding.RssAnnoucementsItemBinding
import ie.equalit.ceno.home.sessioncontrol.HomePageInteractor

class CenoRSSAnnouncementViewHolder(
    itemView: View,
    interactor: HomePageInteractor
) : BaseHomeCardViewHolder(itemView, interactor) {

    private val binding = RssAnnoucementsItemBinding.bind(itemView)

    fun bind(response: RssAnnouncementResponse) {

        HtmlCompat.fromHtml("<u>${response.title}</u>", HtmlCompat.FROM_HTML_MODE_LEGACY).let {
            binding.rssTitle.text = it
        }

        binding.rssTitle.setOnClickListener {
//            openToBrowser(response.link, newTab = true)
        }

        // would fix this logic in a bit to prevent crashes. However, this ViewHolder will never get called if the list is empty
        binding.itemDate.text = response.items[0].pubDate

        "${response.items[0].title} ${response.items[0].description}".let {
            binding.tvMessage.text = it
        }
    }

    companion object {
        val homepageCardType = HomepageCardType.ANNOUNCEMENTS_CARD
    }
}