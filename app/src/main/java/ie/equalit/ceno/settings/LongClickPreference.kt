package ie.equalit.ceno.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnLongClickListener
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder


class LongClickPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    private var longClickListener = OnLongClickListener { v: View? -> true }
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.setOnLongClickListener(this.longClickListener)
    }

    fun onLongClick(longClickListener: OnLongClickListener?) {
        this.longClickListener = longClickListener!!
    }
}