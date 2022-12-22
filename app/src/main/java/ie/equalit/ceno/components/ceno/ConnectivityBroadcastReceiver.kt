package ie.equalit.ceno.components.ceno

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import ie.equalit.ceno.BrowserApplication

object ConnectivityBroadcastReceiver : BroadcastReceiver() {

    const val TAG = "ConnectivityReceiver"

    override fun onReceive(context: Context?, intent: Intent) {
        val info = intent.getParcelableExtra<NetworkInfo>(ConnectivityManager.EXTRA_NETWORK_INFO)
            ?: return

        /* TODO: improve UX for mobile data warning
        if (getConnectionType(context) == MOBILE) {
            if (info.isConnected) {
                logger.debug("Mobile connection detected, showing on mobile data dialog")
                showOnMobileDataDialog()
            } else {
                logger.debug("Mobile connection disabled, hiding on mobile data dialog")
                hideOnMobileDataDialog()
            }
        }
        */

        // Restart the Ouinet client whenever connectivity has changed and become stable.
        val state = info.state
        if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.DISCONNECTED) {
            if (context != null) {
                //logger.debug("Network state changed to ${state}")
                Log.d(TAG, "Stopping OuinetService on connectivity change")
                OuinetService.stopOuinetService(context)
                // TODO: Insert a pause / check client state.
                Log.d(TAG, "Starting OuinetService on connectivity change")
                OuinetService.startOuinetService(context, BrowserApplication.mOuinetConfig)
            }
        }
    }
}