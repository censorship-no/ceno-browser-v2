package ie.equalit.ceno.ext

import android.content.Context
import ie.equalit.ceno.ConsentRequestUi
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
            ui.show(Feature.Lang) { grantedLang ->
                if (grantedLang) {
                    this.grant(Feature.Lang)
                }
                else {
                    this.deny(Feature.Lang)
                }
                ui.show(Feature.Ua) { grantedUa ->
                    if (grantedUa) {
                        this.grant(Feature.Ua)
                    }
                    else {
                        this.deny(Feature.Ua)
                    }
                    callback.invoke(true)
                }
            }
        }
        else {
            this.grant("test")
        }
        callback.invoke(granted)
    }
}
