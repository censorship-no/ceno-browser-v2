/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.home

import android.view.View
import ie.equalit.ceno.databinding.CenoModeItemBinding
import ie.equalit.ceno.home.sessioncontrol.HomePageInteractor
import ie.equalit.ceno.utils.view.CenoViewHolder


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

    fun bind() = Unit

    companion object {
        val homepageCardType = HomepageCardType.MODE_MESSAGE_CARD
    }
}
