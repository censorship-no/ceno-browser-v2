package ie.equalit.ceno.home


import android.os.Build
import android.text.Html
import android.view.View
import ie.equalit.ceno.databinding.RssAnnoucementsItemBinding
import ie.equalit.ceno.home.sessioncontrol.HomePageInteractor

class CenoRSSAnnouncementViewHolder(
    itemView: View,
    interactor: HomePageInteractor
) : BaseHomeCardViewHolder(itemView, interactor) {

    private val binding = RssAnnoucementsItemBinding.bind(itemView)

    fun bind(response: RssAnnouncementResponse) {
        binding.rssTitle.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml("<u>${response.title}</u>", Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml("<u>${response.title}</u>")
        }

        // would fix this logic in a bit to prevent crashes. However, this ViewHolder will never get called if the list is empty
        binding.itemDate.text = response.items[0].pubDate
        "${response.items[0].title} ${response.items[0].description}".let {
            binding.tvMessage.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT)
            } else {
                Html.fromHtml(it)
            }
        }
    }

    companion object {
        val homepageCardType = HomepageCardType.ANNOUNCEMENTS_CARD
    }
}