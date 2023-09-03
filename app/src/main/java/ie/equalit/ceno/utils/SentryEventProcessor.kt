package ie.equalit.ceno.utils

import io.sentry.EventProcessor
import io.sentry.Hint
import io.sentry.SentryEvent

class SentryEventProcessor : EventProcessor {
    override fun process(event: SentryEvent, hint: Hint): SentryEvent? {


        /* TODO: The plan here is to do the following:

        If permissions are allowed and a crash happens, log and return the event
        If permissions are allowed and a non-crash happens (i.e ANR), log and return the event
        If permissions are NOT allowed and a crash happens, save a variable in SharedPreferences for next-launch nudge action and return null
        If permissions are NOT allowed and a non-crash event happens (i.e ANR), return null

        if (CustomPreferenceManager.getBoolean(this, R.string.pref_key_allow_error_reporting, true)) {

        }

        */

        // Check if the event represents a crash (unhandled exception)
        if (event.exceptions?.isNotEmpty() == true) {
            // You can add custom logic here to decide whether to report the crash or not.
            // For example, you can use event data to filter based on specific conditions.

            // To prevent reporting, return null
            return null
        }

        // For non-crash events, return the event for reporting
        return event
    }
}