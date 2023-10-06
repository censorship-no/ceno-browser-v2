package ie.equalit.ceno.home.personal

import android.view.View
import ie.equalit.ceno.databinding.CenoModeItemBinding
import ie.equalit.ceno.databinding.PersonalModeDescriptionBinding
import ie.equalit.ceno.home.HomepageCardType
import ie.equalit.ceno.utils.view.CenoViewHolder

class PersonalModeDescriptionViewHolder(
    view: View
): CenoViewHolder(view) {

    private val binding = PersonalModeDescriptionBinding.bind(view)

    fun bind() = Unit
    companion object {
        val homepageCardType = HomepageCardType.PERSONAL_MODE_CARD
    }
}