package ie.equalit.ceno.tooltip

import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import ie.equalit.ceno.R
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import uk.co.samuelwall.materialtaptargetprompt.extras.PromptFocal
import uk.co.samuelwall.materialtaptargetprompt.extras.PromptTextWithBtn

class CenoTooltip(
    var fragment: Fragment,
    var target: Int,
    var primaryText: String,
    var secondaryText: String,
    var promptFocal: PromptFocal,
    isAutoFinish: Boolean = false,
    stopCaptureTouchOnFocal: Boolean = false,
    val buttonText: Int = R.string.btn_next,
    listener: ( prompt:MaterialTapTargetPrompt, state:Int) -> Unit,
    var onNextButtonPressListener: (view:View) -> Unit
) {
    var tooltip: MaterialTapTargetPrompt?
    val tooltipBuilder : MaterialTapTargetPrompt.Builder
    val promptTextWithBtn = PromptTextWithBtn(
        fragment.resources.displayMetrics.density * 40f
    )
    var btnLocationY: Int = 0

    init {
        tooltipBuilder = MaterialTapTargetPrompt.Builder(fragment)
            .setTarget(target)
            .setPrimaryText(primaryText)
            .setPrimaryTextSize(R.dimen.tooltip_primary_text_size)
            .setSecondaryTextSize(R.dimen.tooltip_secondary_text_size)
            .setSecondaryText(secondaryText)
            .setPromptFocal(promptFocal)
            .setPromptText(promptTextWithBtn)
            .setAutoDismiss(false)
            .setBackgroundColour(getColor(fragment.requireContext(), R.color.tooltip_prompt_color))
            .setFocalColour(getColor(fragment.requireContext(), R.color.tooltip_focal_color))
            .setPromptBackground(DimmedPromptBackground())
            .setPromptStateChangeListener(listener)
            .setAutoFinish(isAutoFinish)
            .setCaptureTouchEventOnFocal(stopCaptureTouchOnFocal)
            .setCaptureTouchEventOutsidePrompt(true)
        tooltip = tooltipBuilder.create()

    }

//    fun promptButtonCallback(x:Float, y:Float) {
//        if (promptTextWithBtn.isButtonPressed(x, y) && !isButtonPressed) {
//            isButtonPressed = true
//            onButtonPressListener.invoke()
//            //dismiss tooltip
//            dismiss()
//        }
//
//    }

    fun addButtons(isLastTooltip:Boolean = false, onExitListener: View.OnClickListener) {
        val container = fragment.activity?.findViewById<FrameLayout>(R.id.container)
        val prompt = container?.findViewById<View>(R.id.material_target_prompt_view)
        container?.removeView(prompt)
        val tooltipOverlay = View.inflate(fragment.requireContext(), R.layout.tooltip_overlay_layout, null) as ConstraintLayout
        tooltipOverlay.addView(prompt, 0)
        container?.addView(tooltipOverlay)

        val btnNext = tooltipOverlay.findViewById<Button>(R.id.btn_next_tooltip)
        btnLocationY = promptTextWithBtn.btnLocation.y.toInt()
        val constraintSet = ConstraintSet()
        constraintSet.clone(tooltipOverlay)
        constraintSet.connect(R.id.btn_next_tooltip, ConstraintSet.START, R.id.tooltip_overlay_layout, ConstraintSet.START, promptTextWithBtn.btnLocation.x.toInt())
        constraintSet.connect(R.id.btn_next_tooltip, ConstraintSet.TOP, R.id.tooltip_overlay_layout, ConstraintSet.TOP, promptTextWithBtn.btnLocation.y.toInt())
        constraintSet.applyTo(tooltipOverlay)
        btnNext.text = fragment.getString(buttonText)
        btnNext.setOnClickListener(onNextButtonPressListener)

        val btnExit = tooltipOverlay.findViewById<Button>(R.id.btn_skip_tour)
        if(!isLastTooltip) {
            btnExit.setOnClickListener(onExitListener)
            btnExit.visibility = View.VISIBLE
        } else {
            btnExit.visibility = View.GONE
        }

        promptTextWithBtn.onUpdate = {
            if (btnLocationY != promptTextWithBtn.btnLocation.y.toInt()) {
                btnLocationY = promptTextWithBtn.btnLocation.y.toInt()
                val constraintSet = ConstraintSet()
                constraintSet.clone(tooltipOverlay)
                constraintSet.connect(R.id.btn_next_tooltip, ConstraintSet.START, R.id.tooltip_overlay_layout, ConstraintSet.START, promptTextWithBtn.btnLocation.x.toInt())
                constraintSet.connect(R.id.btn_next_tooltip, ConstraintSet.TOP, R.id.tooltip_overlay_layout, ConstraintSet.TOP, promptTextWithBtn.btnLocation.y.toInt())
                constraintSet.applyTo(tooltipOverlay)
            }
        }
    }

    fun dismiss() {
        val container = fragment.activity?.findViewById<FrameLayout>(R.id.container)
        container?.removeView(container.findViewById(R.id.tooltip_overlay_layout))
    }

    fun hideButtons() {
        val overlay = fragment.activity?.findViewById<ConstraintLayout>(R.id.tooltip_overlay_layout)
        overlay?.visibility = View.GONE
    }
}