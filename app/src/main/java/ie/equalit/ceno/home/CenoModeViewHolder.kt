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
    private var mode = BrowsingMode.Normal

    init {
        cardType = homepageCardType
    }

    fun bind(mode: BrowsingMode) {
        this@CenoModeViewHolder.mode = mode
        //modify based on mode
        when(mode) {
            BrowsingMode.Normal -> {
                binding.personalModeCard.setOnClickListener {
                    interactor.onClicked(homepageCardType, mode)
                }

                binding.publicModeCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.ceno_blue_100))
                binding.personalModeCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.ceno_home_card_background_tint))

                binding.publicCheckMark.visibility = View.VISIBLE
                binding.personalCheckMark.visibility = View.INVISIBLE
            }
            BrowsingMode.Personal -> {
                binding.publicModeCard.setOnClickListener {
                    interactor.onClicked(homepageCardType, mode)
                }
                binding.publicModeCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.ceno_purple_800))
                binding.personalModeCard.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.ceno_purple_100))

                binding.ivHomeCardPublic.drawable.setTint(ContextCompat.getColor(itemView.context, R.color.ceno_blue_600))
                binding.ivHomeCardPersonal.drawable.setTint(ContextCompat.getColor(itemView.context, R.color.ceno_purple_600))

                //text color
                binding.tvHomeCardPublicText.setTextColor(ContextCompat.getColor(itemView.context, R.color.ceno_grey_200))
                binding.tvHomeCardPersonalText.setTextColor(ContextCompat.getColor(itemView.context, R.color.ceno_onboarding_text))
                binding.tvHomeCardPublicTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.ceno_blue_300))
                binding.ivHomeCardPublic.drawable.setTint(ContextCompat.getColor(itemView.context, R.color.ceno_blue_300))
                binding.tvHomeCardPersonalTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.ceno_purple_700))
                binding.ivHomeCardPersonal.drawable.setTint(ContextCompat.getColor(itemView.context, R.color.ceno_purple_700))

                binding.publicCheckMark.visibility = View.INVISIBLE
                binding.personalCheckMark.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        val homepageCardType = HomepageCardType.MODE_MESSAGE_CARD
    }
}
