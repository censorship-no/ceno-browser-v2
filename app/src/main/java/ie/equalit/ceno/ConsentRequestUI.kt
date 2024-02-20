package ie.equalit.ceno

import android.app.AlertDialog
import android.content.Context
import android.view.View
import ie.equalit.ceno.base.BaseActivity
import ie.equalit.ceno.settings.Settings
import org.cleaninsights.sdk.Campaign
import org.cleaninsights.sdk.ConsentRequestUiComplete
import org.cleaninsights.sdk.Feature

class ConsentRequestUi(private val activity: BaseActivity) : org.cleaninsights.sdk.ConsentRequestUi {

    override fun show(campaignId: String, campaign: Campaign, complete: ConsentRequestUiComplete) {

        val dialogView = View.inflate(activity, R.layout.clean_insights_nudge_dialog, null)


        AlertDialog.Builder(activity)
            .setView(dialogView)
            .setNegativeButton(R.string.clean_insights_maybe_later) { _, _ -> complete(false) }
            .setPositiveButton(R.string.clean_insights_opt_in) { _, _ ->
                Settings.setCleanInsightsTrackingValue(activity, true)
                complete(true)
            }
            .create()
            .show()
    }

    override fun show(feature: Feature, complete: ConsentRequestUiComplete) {
        val msg = activity.getString(R.string._feature_consent_explanation_, feature.localized(activity))

        AlertDialog.Builder(activity)
            .setTitle(R.string.Your_Consent)
            .setMessage(msg)
            .setNegativeButton(R.string.clean_insights_no) { _, _ -> complete(false) }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                BrowserApplication.cleanInsights.grant(feature)
                complete(true)
            }
            .create()
            .show()
    }
}

fun Feature.localized(context: Context): String {
    when (this) {
        Feature.Lang -> return context.getString(R.string.lang_and_locale)
        Feature.Ua -> return context.getString(R.string.Your_device_type)
    }
}
