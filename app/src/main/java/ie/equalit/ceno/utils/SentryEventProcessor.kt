package ie.equalit.ceno.utils

import android.content.Context
import ie.equalit.ceno.R
import ie.equalit.ceno.settings.CustomPreferenceManager
import io.sentry.EventProcessor
import io.sentry.Hint
import io.sentry.SentryEvent
import org.json.JSONObject
import com.google.gson.Gson

class SentryEventProcessor(val context: Context) : EventProcessor {
    override fun process(event: SentryEvent, hint: Hint): SentryEvent? {

        val isPermissionGranted = CustomPreferenceManager.getBoolean(context, R.string.pref_key_allow_crash_reporting, false)
        val isCrash = event.exceptions?.isNotEmpty() == true

        return when {
            isPermissionGranted -> {
                event
            }
            isCrash -> {

                val gson = Gson()
                val sentryEventJson = gson.toJson(event)

                // Save crash event as a String to local
                CustomPreferenceManager.setString(context, R.string.pref_key_last_crash, JSONObject(sentryEventJson).toString())

                // save a variable to nudge users to activate crash recording on next launch
                CustomPreferenceManager.setBoolean(context, R.string.pref_key_crash_happened, true)
                null
            }
            else -> {
                // There's no need to nudge the user when there's a non-crash (e.g ANR)
                null
            }
        }
    }
}