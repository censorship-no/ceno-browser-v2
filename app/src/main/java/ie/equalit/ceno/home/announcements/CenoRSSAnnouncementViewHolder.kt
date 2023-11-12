package ie.equalit.ceno.home.announcements


import android.view.View
import androidx.core.text.HtmlCompat
import ie.equalit.ceno.databinding.RssAnnoucementsItemBinding
import ie.equalit.ceno.home.BaseHomeCardViewHolder
import ie.equalit.ceno.home.HomepageCardType
import ie.equalit.ceno.home.RssAnnouncementResponse
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
            interactor.onUrlClicked(homepageCardType, response.link)
        }

        binding.rssAnnouncementsRecyclerView.adapter = RssAnnouncementSubAdapter(interactor).apply {
            submitList(response.items)
        }


    }

    companion object {
        val homepageCardType = HomepageCardType.ANNOUNCEMENTS_CARD
    }
}