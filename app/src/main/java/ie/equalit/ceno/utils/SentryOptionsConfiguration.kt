package ie.equalit.ceno.utils

import android.content.Context
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroidOptions

object SentryOptionsConfiguration {

    /**
     * This extension function returns the default configuration for the Ceno browser
     */

    fun getConfig(context: Context): Sentry.OptionsConfiguration<SentryAndroidOptions> = Sentry.OptionsConfiguration<SentryAndroidOptions> {
        it.dsn = "https://313d457370cee4729e88117e6096c042@sentry.ouinet.work/5"
        it.isEnableUserInteractionTracing = true
        it.isAttachScreenshot = true
        it.isAttachViewHierarchy = true
        it.sampleRate = 1.0
        it.profilesSampleRate = 1.0
        it.isAnrEnabled = true
        it.addEventProcessor(SentryEventProcessor(context))
    }

}