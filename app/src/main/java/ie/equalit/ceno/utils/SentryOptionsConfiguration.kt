package ie.equalit.ceno.utils

import android.content.Context
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroidOptions

object SentryOptionsConfiguration {

    /**
     * This extension function returns the default configuration for the Ceno browser
     */

    fun getConfig(context: Context): Sentry.OptionsConfiguration<SentryAndroidOptions> = Sentry.OptionsConfiguration<SentryAndroidOptions> {
        it.dsn = "http://606634f4458e4a2a9c1559b519325ad3@ouinet-runner-0.0x271.eu:9000/2"
        it.isEnableUserInteractionTracing = true
        it.isAttachScreenshot = true
        it.isAttachViewHierarchy = true
        it.sampleRate = 1.0
        it.profilesSampleRate = 1.0
        it.isAnrEnabled = true
        it.addEventProcessor(SentryEventProcessor(context))
    }

}