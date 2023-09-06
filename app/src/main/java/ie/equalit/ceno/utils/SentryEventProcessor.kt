package ie.equalit.ceno.utils

import android.content.Context
import io.sentry.EventProcessor
import io.sentry.Hint
import io.sentry.SentryEvent
import org.json.JSONObject
import com.google.gson.Gson
import ie.equalit.ceno.settings.Settings

class SentryEventProcessor(val context: Context) : EventProcessor {
    override fun process(event: SentryEvent, hint: Hint): SentryEvent? {

        val isPermissionGranted = Settings.isCrashReportingPermissionGranted(context)
        val isCrash = event.exceptions?.isNotEmpty() == true

        return when {
            isPermissionGranted -> {
                event
            }
            isCrash -> {

                val gson = Gson()
                val sentryEventJson = gson.toJson(event)

                // Save crash event as a String to local
                Settings.setLastCrash(context, JSONObject(sentryEventJson).toString())

                // save a variable to nudge users to activate crash recording on next launch
                Settings.setCrashHappenedValue(context, true)
                null
            }
            else -> {
                // There's no need to nudge the user when there's a non-crash (e.g ANR)
                null
            }
        }
    }
}