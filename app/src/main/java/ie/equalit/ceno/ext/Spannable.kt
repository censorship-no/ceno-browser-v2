package ie.equalit.ceno.ext

import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.text.inSpans

/**
 * Helper function for overriding click events on a SpannableString
 */
inline fun SpannableStringBuilder.click(
    underlineText: Boolean = false,
    crossinline onClick: (View) -> Unit,
    builderAction: SpannableStringBuilder.() -> Unit
) = inSpans(
    object : ClickableSpan() {

        override fun onClick(view: View) = onClick(view)

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = underlineText
        }
    },
    builderAction = builderAction
)