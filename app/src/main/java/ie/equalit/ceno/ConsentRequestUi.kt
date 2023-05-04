package ie.equalit.ceno

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import org.cleaninsights.sdk.Campaign
import org.cleaninsights.sdk.ConsentRequestUiComplete
import org.cleaninsights.sdk.Feature
import java.text.DateFormat

class ConsentRequestUi(private val activity: Activity): org.cleaninsights.sdk.ConsentRequestUi {

    companion object {
        @JvmStatic
        val df: DateFormat by lazy {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        }
    }

    override fun show(campaignId: String, campaign: Campaign, complete: ConsentRequestUiComplete) {
        val period = campaign.nextTotalMeasurementPeriod ?: return

        val msg = activity.getString(R.string._measurement_consent_explanation_,
            df.format(period.startDate),
            df.format(period.endDate))

        AlertDialog.Builder(activity)
            .setTitle(R.string.Your_Consent)
            .setMessage(msg)
            .setNegativeButton(R.string.No__sorry_) { _, _ -> complete(false) }
            .setPositiveButton(android.R.string.ok) { _, _ -> complete(true) }
            .create()
            .show()
    }

    override fun show(feature: Feature, complete: ConsentRequestUiComplete) {
        val msg = activity.getString(R.string._feature_consent_explanation_, feature.localized(activity))

        AlertDialog.Builder(activity)
            .setTitle(R.string.Your_Consent)
            .setMessage(msg)
            .setNegativeButton(R.string.No__sorry_) { _, _ -> complete(false) }
            .setPositiveButton(android.R.string.ok) { _, _ -> complete(true) }
            .create()
            .show()
    }
}

fun Feature.localized(context: Context): String {
    when (this) {
        Feature.Lang -> return context.getString(R.string.Your_locale)
        Feature.Ua -> return context.getString(R.string.Your_device_type)
    }
}
