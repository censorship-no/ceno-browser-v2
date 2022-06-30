package org.mozilla.reference.browser.components.ceno

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import org.mozilla.reference.browser.R
import ie.equalit.ouinet.Config
import ie.equalit.ouinet.Ouinet


open class OuinetService : Service(){

    companion object {
        private const val TAG = "OuinetService"
        private const val CONFIG_EXTRA = "config"
        private const val SHOW_PURGE_EXTRA = "show-purge"
        private const val HIDE_PURGE_EXTRA = "hide-purge"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ouinet-notification-channel"

        // To see whether this service is running, you may try this command:
        // adb -s $mi shell dumpsys activity services OuinetService
        fun startOuinetService(context: Context, config: Config?) {
            val intent = Intent(context, OuinetService::class.java)
            intent.putExtra(CONFIG_EXTRA, config)
            context.startService(intent)
        }

        fun stopOuinetService(context: Context) {
            val intent = Intent(context, OuinetService::class.java)
            context.stopService(intent)
        }
    }
    private var mOuinet: Ouinet? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
    }

    // added to suppress PendingIntent error TODO: fix
    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.hasExtra(HIDE_PURGE_EXTRA)) {
            Log.d(TAG, "Hiding purge action, intent:$intent")
            startForeground(NOTIFICATION_ID, createNotification(false))
            return START_NOT_STICKY
        }
        if (intent.hasExtra(SHOW_PURGE_EXTRA)) {
            Log.d(TAG, "Showing purge action, intent:$intent")
            startForeground(NOTIFICATION_ID, createNotification(true))

            // Show notification without purge action after some time.
            val hidePurgePIntent = PendingIntent.getService(this, 0,
                    createHidePurgeIntent(this),
                    PendingIntent.FLAG_CANCEL_CURRENT)
            Handler(Looper.myLooper()!!).postDelayed(Runnable {
                try {
                    hidePurgePIntent.send()
                } catch (ce: PendingIntent.CanceledException) {
                }
            }, 3000 /* ms */)
            return START_NOT_STICKY
        }
        Log.d(TAG, "Service starting, intent:$intent")
        require(intent.hasExtra(CONFIG_EXTRA)) { "Service intent missing config extra" }
        val config = intent.getParcelableExtra<Config>(CONFIG_EXTRA)
        synchronized(this) {
            if (mOuinet != null) {
                Log.d(TAG, "Service already started.")
                return START_NOT_STICKY
            }
            mOuinet = Ouinet(this, config)
        }
        startForeground(NOTIFICATION_ID, createNotification(false))
        startOuinet()
        return START_NOT_STICKY
    }

    private fun startOuinet() {
        Thread(Runnable {
            synchronized(this@OuinetService) {
                if (mOuinet == null) return@Runnable
                // Start Ouinet and set proxy in a different thread to avoid strict mode violations.
                setProxyProperties()
                mOuinet!!.start()
            }
        }).start()
    }

    private fun setProxyProperties() {
        Log.d(TAG, "Setting proxy system properties")
        System.setProperty("http.proxyHost", "127.0.0.1")
        System.setProperty("http.proxyPort", "8077")
        System.setProperty("https.proxyHost", "127.0.0.1")
        System.setProperty("https.proxyPort", "8077")
    }

    private fun createHomeIntent(context: Context): Intent {
        // Intent characteristics from `GeckoApp.launchOrBringToFront`.
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("about:home")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        intent.setClassName("org.mozilla.reference-browser",//AppConstants.ANDROID_PACKAGE_NAME,
                "org.mozilla.reference-browser.BrowserApplication")//AppConstants.MOZ_ANDROID_BROWSER_INTENT_CLASS)
        return intent
    }

    private fun createShowPurgeIntent(context: Context): Intent {
        val intent = Intent(context, OuinetService::class.java)
        intent.putExtra(SHOW_PURGE_EXTRA, 1)
        return intent
    }

    private fun createHidePurgeIntent(context: Context): Intent {
        val intent = Intent(context, OuinetService::class.java)
        intent.putExtra(HIDE_PURGE_EXTRA, 1)
        return intent
    }

    // added two more lint suppressors? TODO: fix issues correctly
    @SuppressLint("NewApi", "UnspecifiedImmutableFlag", "LaunchActivityFromNotification")
    private fun createNotification(showRealPurgeAction: Boolean): Notification? {
        var channel_id = CHANNEL_ID
        if (true) { //!AppConstants.Versions.preO) {
            // Create a notification channel for Ouinet notifications. Recreating a notification
            // that already exists has no effect.
            val channel = NotificationChannel(CHANNEL_ID,
                    getString(R.string.ceno_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW)
            channel_id = channel.id
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        var requestCode = 0
        val stopPIntent = PendingIntent.getBroadcast(this, requestCode++,
                OuinetBroadcastReceiver.createStopIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT)
        val homePIntent = PendingIntent.getActivity(this, requestCode++,
                createHomeIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT)
        val showPurgePIntent = PendingIntent.getService(this, requestCode++,
                createShowPurgeIntent(this)!!,
                PendingIntent.FLAG_UPDATE_CURRENT)
        val notifb: NotificationCompat.Builder = NotificationCompat.Builder(this, channel_id!!)
                .setSmallIcon(R.drawable.ic_icon_foreground) //ic_status_logo)
                .setContentTitle(getString(R.string.ceno_notification_title))
                .setContentText(getString(R.string.ceno_notification_description))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(stopPIntent)
                .setAutoCancel(true) // Close on tap.
                .addAction(R.drawable.ic_icon_foreground, //ic_globe_pm,
                        getString(R.string.ceno_notification_home_description),
                        homePIntent)
                .addAction(R.drawable.ic_icon_foreground, //ic_cancel_pm,
                        getString(R.string.ceno_notification_purge_description),
                        showPurgePIntent)
        if (showRealPurgeAction) {
            val purgePIntent = PendingIntent.getBroadcast(this, requestCode++,
                    OuinetBroadcastReceiver.createPurgeIntent(this),
                    PendingIntent.FLAG_UPDATE_CURRENT)
            notifb.addAction(R.drawable.ic_icon_foreground, //ic_cancel_pm,
                    getString(R.string.ceno_notification_purge_do_description),
                    purgePIntent)
        }
        return notifb.build()
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroying service")
        synchronized(this) {
            if (mOuinet != null) {
                val ouinet: Ouinet = mOuinet as Ouinet
                mOuinet = null
                val thread = Thread { ouinet.stop() }
                thread.start()
                try {
                    // Wait a little to allow ouinet to finish gracefuly
                    thread.join(3000 /* ms */)
                } catch (ex: Exception) {
                }
            }
        }
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(p0: Intent?): IBinder? {
        //TODO("Not yet implemented")
        return null
    }
}