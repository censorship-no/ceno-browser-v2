package uk.co.samuelwall.materialtaptargetprompt.extras

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.text.Layout
import android.text.TextPaint
import android.util.Log

class PromptTextWithBtn(
    val buttonHeight: Float,
):PromptText() {

    var textSeparation : Float = 0.0f
    var btnLocation = PointF()

    var mPaintButtonText : TextPaint = TextPaint()

    var onUpdate: (() -> Unit)? = null

    override fun prepare(
        options: PromptOptions<out PromptOptions<*>>,
        clipToBounds: Boolean,
        clipBounds: Rect
    ) {
        super.prepare(options, clipToBounds, clipBounds)
        mPaintButtonText.setColor(Color.BLACK)
        mPaintButtonText.setTextSize(options.secondaryTextSize)

        textSeparation = options.textSeparation

        val verticalTextPositionAbove = options.promptFocal.bounds.centerY() > clipBounds.centerY()
        if (verticalTextPositionAbove) {
            mPrimaryTextTop -= buttonHeight + (2*textSeparation)
            mTextBounds.top -= buttonHeight + (2*textSeparation)
        } else {
            mTextBounds.bottom += buttonHeight + (2*textSeparation)
        }

        btnLocation.x = mPrimaryTextLeft
        btnLocation.y = mTextBounds.top + mPrimaryTextLayout.height + textSeparation + mSecondaryTextLayout.height + (textSeparation *2)

        onUpdate?.invoke()
    }

}