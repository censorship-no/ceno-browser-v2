package ie.equalit.ceno.browser.notification

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ie.equalit.ceno.BrowserActivity
import ie.equalit.ceno.R
import ie.equalit.ceno.ext.components
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
        setContentIntent(getTapIntent())
        addAction(R.drawable.ic_clear_ceno, "STOP", getStopIntent())
        addAction(R.drawable.ic_clear_ceno, "CLEAR", getClearIntent())
    }

    private fun getTapIntent(): PendingIntent? {
        val intent = Intent(this, BrowserActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            action = ACTION_TAP
        }
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    override fun notifyLocaleChanged() {
        super.refreshNotification()
    }

    fun getStopIntent():PendingIntent {
        Intent().also { intent ->
            intent.action = ACTION_STOP
            intent.setPackage(packageName)
            return PendingIntent.getBroadcast(
                this.applicationContext,
                0,
                intent,
                getFlags()
            )
        }
    }

    fun getClearIntent():PendingIntent {
        Intent().also { intent ->
            intent.action = ACTION_CLEAR
            intent.setPackage(packageName)
            return PendingIntent.getBroadcast(
                this.applicationContext,
                0,
                intent,
                getFlags()
            )
        }
    }

    private fun getFlags() : Int {
        var flags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return flags
    }
}