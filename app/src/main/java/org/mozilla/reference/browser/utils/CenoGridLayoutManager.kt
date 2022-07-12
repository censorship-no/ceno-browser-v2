package org.mozilla.reference.browser.utils

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A GridLayoutManager that can be used to override methods in Android implementation
 * to improve ayy1 or fix a11y issues.
 */
class CenoGridLayoutManager(
    context: Context,
    spanCount: Int
) : GridLayoutManager(
    context,
    spanCount
) {
    override fun getColumnCountForAccessibility(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        return if (itemCount < spanCount) {
            itemCount
        } else {
            super.getColumnCountForAccessibility(recycler, state)
        }
    }
}