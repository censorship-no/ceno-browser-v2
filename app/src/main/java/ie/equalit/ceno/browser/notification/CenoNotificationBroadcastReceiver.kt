package ie.equalit.ceno.browser.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CenoNotificationBroadcastReceiver(listener: NotificationListener): BroadcastReceiver() {

    private var listener : NotificationListener? = null

    init {
        this.listener = listener
    }

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        when(intent.action) {
            AbstractPublicNotificationService.ACTION_STOP -> {
                listener?.onStopTapped()
            }
            AbstractPublicNotificationService.ACTION_CLEAR -> {
                listener?.onClearTapped()
            }
        }
    }

    interface NotificationListener {
        fun onStopTapped()

        fun onClearTapped()
    }

}