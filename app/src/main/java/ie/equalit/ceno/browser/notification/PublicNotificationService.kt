package ie.equalit.ceno.browser.notification

import android.annotation.SuppressLint
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ie.equalit.ceno.R
import ie.equalit.ceno.components.PermissionHandler
import ie.equalit.ceno.ext.components
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore

class PublicNotificationService:AbstractPublicNotificationService() {
    override val store: BrowserStore by lazy { components.core.store }

    override fun NotificationCompat.Builder.buildNotification() {
        setSmallIcon(R.drawable.ic_notification)
        setContentTitle(
            getString(R.string.ceno_notification_title)
        )
        setContentText(
            getString(R.string.ceno_notification_description)
        )
        color = ContextCompat.getColor(
            this@PublicNotificationService,
            R.color.ceno_blue_500,
        )
    }

    override fun notifyLocaleChanged() {
        super.refreshNotification()
    }
}