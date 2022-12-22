package ie.equalit.ceno.components.ceno

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import ie.equalit.ceno.R
import ie.equalit.ouinet.Config
import ie.equalit.ouinet.Ouinet
import ie.equalit.ceno.BrowserActivity
import ie.equalit.ceno.BuildConfig
import ie.equalit.ceno.browser.CenoHomeFragment


open class OuinetService : Service(){

    companion object {
        private const val TAG = "OuinetService"
        private const val CONFIG_EXTRA = "config"
        private const val SHOW_PURGE_EXTRA = "show-purge"
        private const val HIDE_PURGE_EXTRA = "hide-purge"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ouinet-notification-channel"
        const val URI_EXTRA = "uri"
        const val CLOSE_EXTRA = "close_activity"

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

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        var flags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        if (intent.hasExtra(HIDE_PURGE_EXTRA)) {
            Log.d(TAG, "Hiding purge action, intent:$intent")
            try {
                startForeground(NOTIFICATION_ID, createNotification(false))
            } catch (_: Exception) {
                stopSelf()
            }
            return START_NOT_STICKY
        }
        if (intent.hasExtra(SHOW_PURGE_EXTRA)) {
            Log.d(TAG, "Showing purge action, intent:$intent")
            try {
                startForeground(NOTIFICATION_ID, createNotification(true))
            } catch (_: Exception) {
                stopSelf()
            }

            // Show notification without purge action after some time.
            val hidePurgePIntent = PendingIntent.getService(this, 0,
                    createHidePurgeIntent(this),
                    flags)
            Handler(Looper.myLooper()!!).postDelayed(Runnable {
                try {
                    hidePurgePIntent.send()
                } catch (ce: PendingIntent.CanceledException) {
                }
            }, 3000 /* ms */)
            return START_NOT_STICKY
        }
        Log.d(TAG,  "Service starting, intent:$intent")
        require(intent.hasExtra(CONFIG_EXTRA)) { "Service intent missing config extra" }
        val config = intent.getParcelableExtra<Config>(CONFIG_EXTRA)
        synchronized(this) {
            if (mOuinet != null) {
                Log.d(TAG,  "Service already started.")
                return START_NOT_STICKY
            }
            mOuinet = Ouinet(this, config)
        }
        try {
            startForeground(NOTIFICATION_ID, createNotification(false))
            startOuinet()
        }
        catch(ex : Exception) {
            /* Stop the service, so it can be restarted in onResume */
            stopSelf()
        }
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

    private fun stopOuinet() {
        val thread = Thread(Runnable {
            synchronized(this@OuinetService) {
                if (mOuinet == null) return@Runnable
                val ouinet: Ouinet = mOuinet as Ouinet
                mOuinet = null
                ouinet.stop()
            }
        })
        thread.start()
        try {
            // Wait a little to allow ouinet to finish gracefuly
            Log.d(TAG, "Wait for ouinet to stop")
            thread.join(10000 /* ms */) // average stop takes 5 seconds
            Log.d(TAG, "Ouinet stop finished")
        } catch ( ex : Exception) {
            Log.d(TAG, "Ouinet stop failed got exception: $ex")
        }
    }

    private fun setProxyProperties() {
        Log.d(TAG, "Setting proxy system properties")
        System.setProperty("http.proxyHost", "127.0.0.1")
        System.setProperty("http.proxyPort", BuildConfig.PROXY_PORT)
        System.setProperty("https.proxyHost", "127.0.0.1")
        System.setProperty("https.proxyPort", BuildConfig.PROXY_PORT)
    }

    private fun createHomeIntent(context: Context): Intent {
        /* Bring BrowserActivity to front and provide homepage uri to be opened */
        val intent = Intent(this, BrowserActivity::class.java)
        intent.putExtra(URI_EXTRA, CenoHomeFragment.ABOUT_HOME)
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
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
    @SuppressLint("LaunchActivityFromNotification")
    private fun createNotification(showRealPurgeAction: Boolean): Notification? {
        var channel_id = CHANNEL_ID
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
            // Create a notification channel for Ouinet notifications. Recreating a notification
            // that already exists has no effect.
            val channel = NotificationChannel(
                CHANNEL_ID,
                    getString(R.string.ceno_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW)
            channel_id = channel.id
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        var requestCode = 0
        val stopPIntent = PendingIntent.getBroadcast(this, requestCode++,
            OuinetBroadcastReceiver.createStopIntent(this),
            flags
        )
        val homePIntent = PendingIntent.getActivity(this, requestCode++,
            createHomeIntent(this),
            flags
        )
        val showPurgePIntent = PendingIntent.getService(this, requestCode++,
            createShowPurgeIntent(this),
            flags
        )
        val notifb: NotificationCompat.Builder = NotificationCompat.Builder(this, channel_id!!)
            .setSmallIcon(R.drawable.ic_status_logo)
            .setContentTitle(getString(R.string.ceno_notification_title))
            .setContentText(getString(R.string.ceno_notification_description))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(stopPIntent)
            .setAutoCancel(true) // Close on tap.
            .addAction(R.drawable.ic_globe_pm,
                getString(R.string.ceno_notification_home_description),
                homePIntent)
            .addAction(R.drawable.ic_cancel_pm,
                getString(R.string.ceno_notification_clear_description),
                showPurgePIntent)
        if (showRealPurgeAction) {
            val purgePIntent = PendingIntent.getBroadcast(this, requestCode++,
                OuinetBroadcastReceiver.createPurgeIntent(this),
                flags
            )
            notifb.addAction(R.drawable.ic_cancel_pm,
                getString(R.string.ceno_notification_clear_do_description),
                purgePIntent)
        }
        return notifb.build()
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroying service")
        stopOuinet()
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(p0: Intent?): IBinder? {
        //TODO("Not yet implemented")
        return null
    }
}