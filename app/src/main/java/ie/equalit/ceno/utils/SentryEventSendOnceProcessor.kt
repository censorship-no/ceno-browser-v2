package ie.equalit.ceno.utils

import io.sentry.EventProcessor
import io.sentry.Hint
import io.sentry.SentryEvent

class SentryEventSendOnceProcessor : EventProcessor {
    override fun process(event: SentryEvent, hint: Hint): SentryEvent {
        return event
    }
}