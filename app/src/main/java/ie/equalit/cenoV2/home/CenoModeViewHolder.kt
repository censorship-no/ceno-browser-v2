/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.cenoV2.home

import android.view.View
import ie.equalit.cenoV2.R
import ie.equalit.cenoV2.databinding.CenoModeItemBinding
import ie.equalit.cenoV2.home.sessioncontrol.CenoModeInteractor
import ie.equalit.cenoV2.utils.view.CenoViewHolder

/**
 * View holder for the CENO normal/private mode description.
 */
class CenoModeViewHolder(
    view: View,
    private val interactor: CenoModeInteractor
) : CenoViewHolder(view) {

    private val binding = CenoModeItemBinding.bind(view)

    init {
        binding.cenoModeItem.setOnClickListener {
            interactor.onCenoModeClicked()
        }
    }

    fun bind() = Unit

    companion object {
        const val LAYOUT_ID = R.layout.ceno_mode_item
    }
}
