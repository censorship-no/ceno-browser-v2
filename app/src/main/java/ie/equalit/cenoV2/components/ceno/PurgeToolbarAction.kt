/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.cenoV2.components.ceno

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mozilla.components.concept.toolbar.Toolbar
import ie.equalit.cenoV2.R
import mozilla.components.support.base.android.Padding
import mozilla.components.support.ktx.android.content.res.resolveAttribute
import mozilla.components.support.ktx.android.view.setPadding

/**
 * A [Toolbar.Action] implementation for CENO purge toolbar button.
 */
@Suppress("LongParameterList")
open class PurgeToolbarAction (
    internal val padding: Padding? = null,
    private val listener: () -> Unit,
) : Toolbar.Action {

    override fun createView(parent: ViewGroup): View {
        val rootView = LayoutInflater.from(parent.context)
            .inflate(R.layout.purge_toolbar_action, parent, false)

        rootView.setOnClickListener { listener.invoke() }

        val backgroundResource =
            parent.context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless)

        rootView.setBackgroundResource(backgroundResource)
        padding?.let { rootView.setPadding(it) }

        return rootView
    }

    override fun bind(view: View) {
        /* Nothing to bind */
    }
}
