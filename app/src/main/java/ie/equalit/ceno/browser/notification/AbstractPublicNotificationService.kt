package ie.equalit.ceno.browser.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_SECRET
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import ie.equalit.ceno.R
import ie.equalit.ceno.components.PermissionHandler
import ie.equalit.ceno.ext.ifChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.collect
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.ids.SharedIdsHelper
import mozilla.components.support.ktx.android.notification.ChannelData
import mozilla.components.support.ktx.android.notification.ensureNotificationChannelExists
import mozilla.components.support.utils.PendingIntentUtils
import mozilla.components.support.utils.ext.stopForegroundCompat
import java.util.Locale

abstract class AbstractPublicNotificationService : Service() {

    private var publicTabScope: CoroutineScope? = null
    private var localeScope: CoroutineScope? = null

    abstract val store: BrowserStore

    /**
     * Customizes the private browsing notification.
     */
    abstract fun NotificationCompat.Builder.buildNotification()

    /**
     * Customize the notification response when the [Locale] has been changed.
     */
    abstract fun notifyLocaleChanged()

    /**
     * Erases all public tabs in reaction to the user tapping the notification.
     */
    @CallSuper
    protected open fun erasePublicTabs() {
        store.dispatch(TabListAction.RemoveAllNormalTabsAction)
    }

    /**
     * Retrieves the notification id based on the tag.
     */
    protected fun getNotificationId(): Int {
        return SharedIdsHelper.getIdForTag(this, NOTIFICATION_TAG)
    }

    /**
     * Retrieves the channel id based on the channel data.
     */
    protected fun getChannelId(): String {
        return ensureNotificationChannelExists(
            this,
            NOTIFICATION_CHANNEL,
            onSetupChannel = {
                if (SDK_INT >= Build.VERSION_CODES.O) {
                    enableLights(false)
                    enableVibration(false)
                    setShowBadge(false)
                }
            },
        )
    }

    /**
     * Re-build and notify an existing notification.
     */
    protected fun refreshNotification() {
        val notificationId = getNotificationId()
        val channelId = getChannelId()

        val notification = createNotification(channelId)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //do nothing if permission not granted
            return
        }
        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
    }

    /**
     * Create the private browsing notification and
     * add a listener to stop the service once all private tabs are closed.
     *
     * The service should be started only if private tabs are open.
     */
    @SuppressLint("ForegroundServiceType")
    final override fun onCreate() {
        val notificationId = getNotificationId()
        val channelId = getChannelId()
        val notification = createNotification(channelId)

        startForeground(notificationId, notification)

        publicTabScope = store.flowScoped { flow ->
            flow.map { state -> state.normalTabs.isEmpty() }
                .ifChanged()
                .collect { noPublicTabs ->
                    if (noPublicTabs) stopService()
                }
        }

        localeScope = store.flowScoped { flow ->
            flow.mapNotNull { state -> state.locale }
                .ifChanged()
                .collect {
                    notifyLocaleChanged()
                }
        }
    }

    /**
     * Builds a notification based on the specified channel id.
     *
     * @param channelId The channel id for the [Notification]
     */
    fun createNotification(channelId: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setOngoing(true)
            .setVisibility(VISIBILITY_SECRET)
            .setShowWhen(false)
            .setLocalOnly(true)
            .setContentIntent(
                Intent(ACTION_ERASE).let {
                    it.setClass(this, this::class.java)
                    PendingIntent.getService(this, 0, it, PendingIntentUtils.defaultFlags or FLAG_ONE_SHOT)
                },
            )
            .apply { buildNotification() }
            .build()
    }

    final override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action == ACTION_ERASE) {
            erasePublicTabs()
        }

        return START_NOT_STICKY
    }

    final override fun onDestroy() {
        publicTabScope?.cancel()
        localeScope?.cancel()
    }

    final override fun onBind(intent: Intent?): IBinder? = null

    private fun stopService() {
        stopForegroundCompat(true)
        stopSelf()
    }

    companion object {
        private const val NOTIFICATION_TAG =
            "ie.equalit.ceno.browser.notification.AbstractPublicNotificationService"
        const val ACTION_ERASE = "ie.equalit.ceno.browser.notification.action.ERASE"

        val NOTIFICATION_CHANNEL = ChannelData(
            id = "browsing-session",
            name = R.string.ceno_notification_channel_name,
            importance = IMPORTANCE_LOW,
        )
    }
}