package ie.equalit.ceno.home.personal

import android.os.Build
import android.text.Html
import android.view.View
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.CenoModeItemBinding
import ie.equalit.ceno.databinding.PersonalModeDescriptionBinding
import ie.equalit.ceno.home.HomepageCardType
import ie.equalit.ceno.utils.view.CenoViewHolder

class PersonalModeDescriptionViewHolder(
    view: View
): CenoViewHolder(view) {

    private val binding = PersonalModeDescriptionBinding.bind(view)

    init {
        binding.tvPersonalBrowsingLearnMore.setOnClickListener {
            //open page todo
        }
    }

    fun bind() = Unit
    companion object {
        val homepageCardType = HomepageCardType.PERSONAL_MODE_CARD
    }
}