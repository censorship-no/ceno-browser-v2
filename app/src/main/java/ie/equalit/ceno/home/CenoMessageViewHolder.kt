package ie.equalit.ceno.home

import android.util.Log
import android.view.View
import ie.equalit.ceno.databinding.HomepageCardItemBinding
import ie.equalit.ceno.utils.view.CenoViewHolder

class CenoMessageViewHolder (
    itemView: View
) : CenoViewHolder(itemView) {

    private val binding = HomepageCardItemBinding.bind(itemView)

    init {
        binding.closeButton.setOnClickListener {
            Log.d("Mess", "Home card closed")
        }
    }

    fun bind(message: CenoMessageCard) {
        Log.d("CARD", message.title)
        binding.tvCardTitle.text = message.title
        binding.tvCardText.text = message.text
//        binding.ivMessageIcon.setImageDrawable(message.icon)
    }

    companion object {
        val homepageCardType = HomepageCardType.BASIC_MESSAGE_CARD
    }
}