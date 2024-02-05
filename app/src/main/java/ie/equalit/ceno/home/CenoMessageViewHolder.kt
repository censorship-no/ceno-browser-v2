package ie.equalit.ceno.home

import android.view.View
import ie.equalit.ceno.browser.BrowsingMode
import ie.equalit.ceno.databinding.HomeMessageCardItemBinding
import ie.equalit.ceno.home.sessioncontrol.HomePageInteractor

class CenoMessageViewHolder (
    itemView: View,
    interactor: HomePageInteractor
) : BaseHomeCardViewHolder(itemView, interactor){

    private val binding = HomeMessageCardItemBinding.bind(itemView)

    init {
        cardType = homepageCardType
//        enableContextMenu()

    }

    fun bind(message: CenoMessageCard) {
        binding.tvCardTitle.text = message.title
        binding.tvCardText.text = message.text
        binding.cardHomepage.setOnClickListener {
            interactor.onClicked(cardType, BrowsingMode.Normal)
        }
    }

    companion object {
        val homepageCardType = HomepageCardType.BASIC_MESSAGE_CARD
    }
}