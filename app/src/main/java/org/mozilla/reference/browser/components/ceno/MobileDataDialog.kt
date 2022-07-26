package org.mozilla.reference.browser.components.ceno

import android.app.AlertDialog
import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import androidx.core.app.ActivityCompat
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.reference.browser.BrowserActivity
import org.mozilla.reference.browser.BrowserApplication
import org.mozilla.reference.browser.R
import org.mozilla.reference.browser.settings.Settings

class MobileDataDialog (
        private val context: Context,
        private val activity: BrowserActivity)
    {
    private var mOnMobileDataDialog: AlertDialog? = null
    private val logger = Logger("MobileDataDialog")

    private fun showOnMobileDataDialog() {
        if (Settings.isMobileDataEnabled(context)) {
            logger.debug("Mobile data has been enabled already")
            return
        }
        if (mOnMobileDataDialog == null) {
            logger.debug("First time the on mobile data dialog is called, create.")
            createOnMobileDataDialog()
        }
        if (!(mOnMobileDataDialog!!.isShowing())) {
            logger.debug("Showing on mobile data dialog.")
            mOnMobileDataDialog!!.show()
        }
    }

    private fun hideOnMobileDataDialog() {
        if (mOnMobileDataDialog == null) {
            logger.debug("Not hiding on mobile data dialog, not yet created.")
            return
        }
        if (mOnMobileDataDialog!!.isShowing()) {
            logger.debug("Hiding on mobile data dialog.")
            mOnMobileDataDialog!!.dismiss() // `.hide()` results in it now showing up again
        }
    }

    private fun createOnMobileDataDialog() {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    Logger.debug("Stopping application from on mobile data dialog")
                    /**
                     * This is the preferred way to exit the app, but it is triggering an
                     * exception in the cpp code which brings up the crash handler dialog.
                     * ActivityCompat.finishAffinity(GeckoApp.this);
                     */
                    OuinetService.stopOuinetService(context)
                    ActivityCompat.finishAffinity(activity);
                }
                DialogInterface.BUTTON_NEUTRAL -> Logger.debug("Dismissing on mobile data dialog")
                DialogInterface.BUTTON_NEGATIVE -> {
                    Logger.debug("Stop showing on mobile data dialog button pressed by user")
                    Settings.setMobileData(context, true)
                }
            }
        }
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
                .setTitle(R.string.ceno_on_mobile_data_dialog_title)
                .setMessage(R.string.ceno_on_mobile_data_dialog_description)
                .setPositiveButton(R.string.ceno_on_mobile_data_dialog_stop_now, dialogClickListener)
                .setNeutralButton(R.string.ceno_on_mobile_data_dialog_continue, dialogClickListener)
                .setNegativeButton(R.string.ceno_on_mobile_data_dialog_stop_showing, dialogClickListener)
        mOnMobileDataDialog = dialogBuilder.create()
    }

    fun getConnectionType(context: Context?): Int {
        var result = 0 // Returns connection type. 0: none; 1: mobile data; 2: wifi; 3: vpn
        val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm?.run {
                cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                    if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        result = WIFI
                    } else if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        result = MOBILE
                    } else if (hasTransport(NetworkCapabilities.TRANSPORT_VPN)){
                        result = VPN
                    }
                }
            }
        } else {
            cm?.run {
                cm.activeNetworkInfo?.run {
                    if (type == ConnectivityManager.TYPE_WIFI) {
                        result = WIFI
                    } else if (type == ConnectivityManager.TYPE_MOBILE) {
                        result = MOBILE
                    } else if(type == ConnectivityManager.TYPE_VPN) {
                        result = VPN
                    }
                }
            }
        }
        return result
    }

    /**
     * Initialization of mobile data dialog, setups broadcast receiver to
     * monitor for change in network connectivity
     */
    init {
        /* check if mobile data is the active connection type */
        if (getConnectionType(context) == MOBILE) {
            showOnMobileDataDialog()
        }

        /* The below method for accessing detecting connectivity changes has been deprecated as of API 28
         * should replace with the more flexible NetworkRequestCallback, but that requires min API 23
         */
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val info = intent.getParcelableExtra<NetworkInfo>(ConnectivityManager.EXTRA_NETWORK_INFO)
                        ?: return
                if (getConnectionType(context) == MOBILE) {
                    if (info.isConnected) {
                        logger.debug("Mobile connection detected, showing on mobile data dialog")
                        showOnMobileDataDialog()
                    } else {
                        logger.debug("Mobile connection disabled, hiding on mobile data dialog")
                        hideOnMobileDataDialog()
                    }
                }

                // Restart the Ouinet client whenever connectivity has changed and become stable.
                val state = info.state
                if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.DISCONNECTED) {
                    if (context != null) {
                        logger.debug("Network state chagned to ${state}")
                        OuinetService.stopOuinetService(context)
                        // TODO: Insert a pause / check client state.
                        OuinetService.startOuinetService(context, BrowserApplication.mOuinetConfig)
                    }
                }
            }
        }, intentFilter)
    }

    companion object {
        const val MOBILE = 1
        const val WIFI = 2
        const val VPN = 2
        private const val TAG = "MDDService"
    }
}