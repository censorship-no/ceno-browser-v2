package ie.equalit.ceno.home

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import ie.equalit.ceno.R
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
        binding.tvCardTitle.setOnClickListener {

            if(binding.btnGoToSetting.isVisible) {
                //collapse
                binding.btnGoToSetting.visibility = View.GONE
                binding.tvCardText.visibility = View.GONE
                binding.tvCardTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(itemView.context, R.drawable.ic_arrow_collapsed), null)
            } else {
                //expand
                binding.btnGoToSetting.visibility = View.VISIBLE
                binding.tvCardText.visibility = View.VISIBLE
                binding.tvCardTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(itemView.context, R.drawable.ic_arrow_expanded), null)
            }

        }
        binding.btnGoToSetting.setOnClickListener {
            interactor.onClicked(cardType, BrowsingMode.Normal)
        }
    }

    companion object {
        val homepageCardType = HomepageCardType.BASIC_MESSAGE_CARD
    }
}