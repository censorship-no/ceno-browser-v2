/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.home

import android.view.View
import androidx.core.content.ContextCompat
import ie.equalit.ceno.R
import ie.equalit.ceno.browser.BrowsingMode
import ie.equalit.ceno.databinding.CenoModeItemBinding
import ie.equalit.ceno.home.sessioncontrol.HomePageInteractor


/**
 * View holder for the CENO normal/private mode description.
 */
class CenoModeViewHolder(
    view: View,
    interactor: HomePageInteractor
) : BaseHomeCardViewHolder(view, interactor) {

    private val binding = CenoModeItemBinding.bind(view)

    init {
        cardType = homepageCardType
        binding.personalModeCard.setOnClickListener {
            interactor.onClicked(homepageCardType)
        }
    }

    fun bind(mode: BrowsingMode) {
        //modify based on mode
        when(mode) {
            BrowsingMode.Normal -> {
                binding.publicModeCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.ceno_home_card_background_tint))
                binding.personalModeCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.ceno_home_card_background_tint))
                binding.publicModeCard.elevation = 2f
                binding.personalModeCard.elevation = 8f

                //text color

            }
            BrowsingMode.Personal -> {
                binding.cenoModeItem.background = ContextCompat.getDrawable(itemView.context, R.color.fx_mobile_private_layer_color_1)
                binding.publicModeCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.fx_mobile_private_layer_color_accent_opaque))
                binding.personalModeCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.fx_mobile_private_layer_color_accent_opaque))
                binding.publicModeCard.elevation = 8f
                binding.personalModeCard.elevation = 2f

                //text color
                binding.tvHomeCardPublicText.setTextColor(ContextCompat.getColor(itemView.context, R.color.fx_mobile_private_text_color_primary))
                binding.tvHomeCardPersonalText.setTextColor(ContextCompat.getColor(itemView.context, R.color.fx_mobile_private_text_color_primary))

            }
        }
    }

    companion object {
        val homepageCardType = HomepageCardType.MODE_MESSAGE_CARD
    }
}
