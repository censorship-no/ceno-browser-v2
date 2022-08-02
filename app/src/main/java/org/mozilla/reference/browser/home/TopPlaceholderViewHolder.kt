/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser.home

import android.view.View
import org.mozilla.reference.browser.R
import org.mozilla.reference.browser.utils.view.CenoViewHolder

/**
 * View holder for a synchronous, unconditional and invisible placeholder.  This is to anchor home to
 * the top when home is created.
 */
class TopPlaceholderViewHolder(
    view: View
) : CenoViewHolder(view) {

    fun bind() = Unit

    companion object {
        const val LAYOUT_ID = R.layout.top_placeholder_item
    }
}
