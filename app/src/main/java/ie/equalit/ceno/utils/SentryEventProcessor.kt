package ie.equalit.ceno.utils

import android.content.Context
import android.util.Log
import ie.equalit.ceno.R
import ie.equalit.ceno.settings.CustomPreferenceManager
import io.sentry.EventProcessor
import io.sentry.Hint
import io.sentry.SentryEvent

class SentryEventProcessor(val context: Context) : EventProcessor {
    override fun process(event: SentryEvent, hint: Hint): SentryEvent? {

        val isPermissionGranted = CustomPreferenceManager.getBoolean(context, R.string.pref_key_allow_error_reporting, true)
        val isCrash = event.exceptions?.isNotEmpty() == true

        return when {
            isPermissionGranted -> {
                event
            }
            isCrash -> {
                // save a variable to SharedPreferences for next launch
                CustomPreferenceManager.setBoolean(context, R.string.pref_key_crash_happened, true)
                Log.d("PPPPPP", "Crashhhhhhh")
                null
            }
            else -> null
        }
    }
}