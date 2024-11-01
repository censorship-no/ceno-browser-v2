package uk.co.samuelwall.materialtaptargetprompt.extras

import android.graphics.PointF
import android.graphics.Rect

class PromptTextWithBtn(
    val buttonHeight: Float,
):PromptText() {

    var textSeparation : Float = 0.0f
    var btnLocation = PointF()

    var onUpdate: (() -> Unit)? = null

    override fun prepare(
        options: PromptOptions<out PromptOptions<*>>,
        clipToBounds: Boolean,
        clipBounds: Rect
    ) {
        super.prepare(options, clipToBounds, clipBounds)

        textSeparation = options.textSeparation

        val verticalTextPositionAbove = options.promptFocal.bounds.centerY() > clipBounds.centerY()
        if (verticalTextPositionAbove) {
            mPrimaryTextTop -= buttonHeight + textSeparation
            mTextBounds.top -= buttonHeight + textSeparation
        } else {
            mTextBounds.bottom += buttonHeight + (textSeparation)
        }

        btnLocation.x = mPrimaryTextLeft
        btnLocation.y = mTextBounds.top + mPrimaryTextLayout.height + textSeparation + mSecondaryTextLayout.height + textSeparation

        onUpdate?.invoke()
    }

}