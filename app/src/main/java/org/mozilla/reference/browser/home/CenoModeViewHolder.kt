/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser.home

import android.view.View
import org.mozilla.reference.browser.R
import org.mozilla.reference.browser.databinding.CenoModeItemBinding
import org.mozilla.reference.browser.home.sessioncontrol.CenoModeInteractor
import org.mozilla.reference.browser.utils.view.CenoViewHolder

/**
 * View holder for the CENO normal/private mode description.
 */
class CenoModeViewHolder(
    view: View,
    private val interactor: CenoModeInteractor
) : CenoViewHolder(view) {

    private val binding = CenoModeItemBinding.bind(view)

    init {
        binding.textView.setOnClickListener {
            interactor.onCenoModeClicked()
        }
    }

    fun bind() = Unit

    companion object {
        const val LAYOUT_ID = R.layout.ceno_mode_item
    }
}
