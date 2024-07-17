package ie.equalit.ceno

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import ie.equalit.ceno.base.BaseActivity
import ie.equalit.ceno.ext.click
import org.cleaninsights.sdk.Campaign
import org.cleaninsights.sdk.ConsentRequestUiComplete
import org.cleaninsights.sdk.Feature
import org.cleaninsights.sdk.ConsentRequestUi

class ConsentRequestUi(private val activity: BaseActivity) :
    ConsentRequestUi {

    override fun show(campaignId: String, campaign: Campaign, complete: ConsentRequestUiComplete) {

        val dialogView = View.inflate(activity, R.layout.clean_insights_nudge_dialog, null)
        dialogView.findViewById<TextView>(R.id.explainer).text = buildSpannedString {

            append(dialogView.context.getString(R.string.clean_insights_explainer))

            color(ContextCompat.getColor(dialogView.context, R.color.accent)) {
                click(false, onClick = {
                    // todo: open popup?
                }) {
                    append(" ")
                    append(dialogView.context.getString(R.string.learn_more_title))
                    append(".")
                }
            }
        }

        AlertDialog.Builder(activity)
            .setView(dialogView)
            .setNegativeButton(R.string.clean_insights_maybe_later) { _, _ -> complete(false) }
            .setPositiveButton(R.string.clean_insights_opt_in) { _, _ ->
                complete(true)
            }
            .create()
            .show()
    }

    override fun show(feature: Feature, complete: ConsentRequestUiComplete) {
        val msg = activity.getString(
            R.string.clean_insights_feature_consent_explanation,
            feature.localized(activity)
        )

        AlertDialog.Builder(activity)
            .setTitle(R.string.clean_insights_header)
            .setMessage(msg)
            .setNegativeButton(R.string.clean_insights_no) { _, _ -> complete(false) }
            .setPositiveButton(R.string.ceno_notification_clear_do_description) { _, _ ->
                BrowserApplication.cleanInsights?.grant(feature)
                complete(true)
            }
            .create()
            .show()
    }
}

fun Feature.localized(context: Context): String {
    return when (this) {
        Feature.Lang -> context.getString(R.string.clean_insights__locale)
        Feature.Ua -> context.getString(R.string.clean_insights_device_type)
    }
}
