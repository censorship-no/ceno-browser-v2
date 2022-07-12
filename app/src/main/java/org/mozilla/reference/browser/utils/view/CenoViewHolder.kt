package org.mozilla.reference.browser.utils.view

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * A base class for all recycler view holders supporting Android Extensions-style view access.
 * This allows views to be used without an `itemView.<id>` prefix, and additionally caches them.
 */
abstract class CenoViewHolder(
    val containerView: View
) : RecyclerView.ViewHolder(containerView)
