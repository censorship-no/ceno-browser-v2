package ie.equalit.ceno.home.personal

import android.os.Build
import android.text.Html
import android.view.View
import androidx.core.content.ContextCompat
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.CenoModeItemBinding
import ie.equalit.ceno.databinding.PersonalModeDescriptionBinding
import ie.equalit.ceno.home.HomepageCardType
import ie.equalit.ceno.home.sessioncontrol.HomePageInteractor
import ie.equalit.ceno.utils.view.CenoViewHolder

class PersonalModeDescriptionViewHolder(
    view: View,
    interactor: HomePageInteractor
): CenoViewHolder(view) {

    private val binding = PersonalModeDescriptionBinding.bind(view)

    init {
        binding.root.setBackgroundColor(ContextCompat.getColor(view.context ,R.color.fx_mobile_private_layer_color_1))
        binding.tvPersonalBrowsingLearnMore.setOnClickListener {
            interactor.onClicked(homepageCardType)

        }
    }

    fun bind() = Unit
    companion object {
        val homepageCardType = HomepageCardType.PERSONAL_MODE_CARD
    }
}