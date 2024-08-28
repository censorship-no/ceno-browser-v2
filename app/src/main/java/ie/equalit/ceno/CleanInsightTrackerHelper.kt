package ie.equalit.ceno

import android.app.Activity
import android.app.AlertDialog
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import ie.equalit.ceno.BrowserApplication.Companion.cleanInsights
import ie.equalit.ceno.BrowserApplication.Companion.getTracker
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper

object CleanInsightTrackerHelper {

    fun showStartupTimePrompt(activity: Activity) {
        val dialogView = View.inflate(activity, R.layout.ouinet_startup_time_prompt, null)
        var recordedValue: Double = 0.0

        val radio1 = dialogView.findViewById<RadioButton>(R.id.radio_1)
        val radio2 = dialogView.findViewById<RadioButton>(R.id.radio_2)
        val radio3 = dialogView.findViewById<RadioButton>(R.id.radio_3)
        val radio4 = dialogView.findViewById<RadioButton>(R.id.radio_4)
        val radio5 = dialogView.findViewById<RadioButton>(R.id.radio_5)

        radio1.setOnClickListener {
            recordedValue = checkSingleRadioButtonInGroup(
                radio1,
                listOf(radio1, radio2, radio3, radio4, radio5)
            )
        }
        radio2.setOnClickListener {
            recordedValue = checkSingleRadioButtonInGroup(
                radio2,
                listOf(radio1, radio2, radio3, radio4, radio5)
            )
        }
        radio3.setOnClickListener {
            recordedValue = checkSingleRadioButtonInGroup(
                radio3,
                listOf(radio1, radio2, radio3, radio4, radio5)
            )
        }
        radio4.setOnClickListener {
            recordedValue = checkSingleRadioButtonInGroup(
                radio4,
                listOf(radio1, radio2, radio3, radio4, radio5)
            )
        }
        radio5.setOnClickListener {
            recordedValue = checkSingleRadioButtonInGroup(
                radio5,
                listOf(radio1, radio2, radio3, radio4, radio5)
            )
        }

        AlertDialog.Builder(activity)
            .setView(dialogView)
            .setNegativeButton(R.string.onboarding_skip_button) { _, _ -> }
            .setPositiveButton(R.string.submit) { _, _ ->

                if (recordedValue != 0.0) {

                    trackData(
                        category = "user-feedback",
                        action = "ouinet_startup-success",
                        campaign = CleanInsightCampaigns.TEST,
                        name = "perceived_ouinet_startup_time",
                        value = recordedValue
                    )
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.thank_you_for_feedback),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .create()
            .show()
    }

    private fun checkSingleRadioButtonInGroup(
        radioToCheck: RadioButton,
        list: List<RadioButton>
    ): Double {
        for (radioButton in list) radioButton.isChecked = false
        radioToCheck.isChecked = true

        return when (radioToCheck.id) {
            R.id.radio_1 -> 1.0
            R.id.radio_2 -> 2.0
            R.id.radio_3 -> 3.0
            R.id.radio_4 -> 4.0
            R.id.radio_5 -> 5.0
            else -> 0.0
        }
    }

    fun trackData(
        activity: String? = null,
        category: String,
        action: String,
        campaign: CleanInsightCampaigns,
        name: String,
        value: Double
    ) {

        cleanInsights?.measureEvent(category, action, campaign.toServerString(), name, value)
//        cleanInsights.measureVisit(listOf("Main"), "test")

        val tracker: Tracker? = getTracker()

        activity?.let { act -> TrackHelper.track().screen("/$act").title(act).with(tracker) }

        TrackHelper.track().event(category, action).value(value.toFloat()).with(tracker)
        TrackHelper.track().download().with(tracker)
    }

    enum class CleanInsightCampaigns() {
        TEST;

        fun toServerString() = when (this) {
            TEST -> "test"
        }
    }
}