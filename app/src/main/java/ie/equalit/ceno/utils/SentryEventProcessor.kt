package ie.equalit.ceno.utils

import android.content.Context
import io.sentry.EventProcessor
import io.sentry.Hint
import io.sentry.SentryEvent
import ie.equalit.ceno.settings.Settings

class SentryEventProcessor(val context: Context) : EventProcessor {
    override fun process(event: SentryEvent, hint: Hint): SentryEvent? {

        val isPermissionGranted = Settings.isCrashReportingPermissionGranted(context)
        val isCrash = event.exceptions?.isNotEmpty() == true

        if(isPermissionGranted && isCrash) {
            Settings.logSuccessfulCrashEventCommit(context, true)
            return event
        }

        return null
    }
}