package ie.equalit.ceno.ext

import android.content.Context
import ie.equalit.ceno.ConsentRequestUi
import ie.equalit.ceno.settings.Settings
import org.cleaninsights.sdk.CleanInsights
import org.cleaninsights.sdk.Feature

/* This function displays the popup that asks users if they want to opt in for
the clean insights reporting
 */
fun CleanInsights.launchCleanInsightsPermissionDialog(context : Context, callback : (Boolean) -> Unit) {

    val ui = ConsentRequestUi(context)

    ui.show() { granted ->
        if (granted) {
            this.grant("test")
            if (Settings.isCleanInsightsDeviceTypeIncluded(context)) {
                this.grant(Feature.Ua)
            }
            if (Settings.isCleanInsightsDeviceLocaleIncluded(context)) {
                this.grant(Feature.Lang)
            }
        }
        else {
            this.deny("test")
        }
        Settings.setCleanInsightsEnabled(context, granted)
        callback.invoke(granted)
    }
}
