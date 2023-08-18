package ie.equalit.ceno.home

import android.util.Log
import android.view.View
import ie.equalit.ceno.databinding.HomepageCardItemBinding
import ie.equalit.ceno.home.sessioncontrol.HomePageInteractor
import ie.equalit.ceno.utils.view.CenoViewHolder

class CenoMessageViewHolder (
    itemView: View,
    val interactor: HomePageInteractor
) : CenoViewHolder(itemView) {

    private val binding = HomepageCardItemBinding.bind(itemView)

    init {
        binding.closeButton.setOnClickListener {
            interactor.onRemoveCard(binding.root, homepageCardType)
        }
    }

    fun bind(message: CenoMessageCard) {
        binding.tvCardTitle.text = message.title
        binding.tvCardText.text = message.text
//        binding.ivMessageIcon.setImageDrawable(message.icon)
    }

    companion object {
        val homepageCardType = HomepageCardType.BASIC_MESSAGE_CARD
    }
}