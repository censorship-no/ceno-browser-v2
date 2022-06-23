/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser

import android.app.AlertDialog
import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import mozilla.components.browser.state.selector.findCustomTabOrSelectedTab
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.WebExtensionState
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.intent.ext.EXTRA_SESSION_ID
import mozilla.components.lib.crash.Crash
import mozilla.components.support.base.feature.ActivityResultHandler
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.webextensions.WebExtensionPopupFeature
import org.mozilla.reference.browser.addons.WebExtensionActionPopupActivity
import org.mozilla.reference.browser.browser.BrowserFragment
import org.mozilla.reference.browser.browser.CrashIntegration
import org.mozilla.reference.browser.browser.OuinetService
import org.mozilla.reference.browser.ext.components
import org.mozilla.reference.browser.ext.isCrashReportActive
import org.mozilla.reference.browser.settings.Settings

/**
 * Activity that holds the [BrowserFragment].
 */
open class BrowserActivity : AppCompatActivity() {

    private lateinit var crashIntegration: CrashIntegration

    private val sessionId: String?
        get() = SafeIntent(intent).getStringExtra(EXTRA_SESSION_ID)

    private val tab: SessionState?
        get() = components.core.store.state.findCustomTabOrSelectedTab(sessionId)

    private val webExtensionPopupFeature by lazy {
        WebExtensionPopupFeature(components.core.store, ::openPopup)
    }

    private var mOnMobileDataDialog: AlertDialog? = null

    open fun showOnMobileDataDialog() {
        if (Settings.isMobileDataEnabled(this)) {
            Logger.debug("Mobile data has been enabled already")
            return
        }
        if (mOnMobileDataDialog == null) {
            Logger.debug("First time the on mobile data dialog is called, create.")
            createOnMobileDataDialog()
        }
        if (!(mOnMobileDataDialog!!.isShowing())) {
            Logger.debug("Showing on mobile data dialog.")
            mOnMobileDataDialog!!.show()
        }
    }

    open fun hideOnMobileDataDialog() {
        if (mOnMobileDataDialog == null) {
            Logger.debug("Not hiding on mobile data dialog, not yet created.")
            return
        }
        if (mOnMobileDataDialog!!.isShowing()) {
            Logger.debug("Hiding on mobile data dialog.")
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
                    OuinetService.stopOuinetService(this@BrowserActivity)
                    ActivityCompat.finishAffinity(this@BrowserActivity);
                }
                DialogInterface.BUTTON_NEUTRAL -> Logger.debug("Dismissing on mobile data dialog")
                DialogInterface.BUTTON_NEGATIVE -> {
                    Logger.debug("Stop showing on mobile data dialog button pressed by user")
                    Settings.setMobileData(this, true)
                }
            }
        }
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
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
                        result = 2
                    } else if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        result = 1
                    } else if (hasTransport(NetworkCapabilities.TRANSPORT_VPN)){
                        result = 3
                    }
                }
            }
        } else {
            cm?.run {
                cm.activeNetworkInfo?.run {
                    if (type == ConnectivityManager.TYPE_WIFI) {
                        result = 2
                    } else if (type == ConnectivityManager.TYPE_MOBILE) {
                        result = 1
                    } else if(type == ConnectivityManager.TYPE_VPN) {
                        result = 3
                    }
                }
            }
        }
        return result
    }

    /**
     * Returns a new instance of [BrowserFragment] to display.
     */
    open fun createBrowserFragment(sessionId: String?): Fragment =
        BrowserFragment.create(sessionId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* check if mobile data is the active connection type */
        if (getConnectionType(this) == 1) {
            showOnMobileDataDialog()
        }

        /* The below method for accessing detecting connectivity changes has been deprecated as of API 28
         * should replace with the more flexible NetworkRequestCallback, but that requires min API 23
         */
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val info = intent.getParcelableExtra<NetworkInfo>(ConnectivityManager.EXTRA_NETWORK_INFO)
                        ?: return
                if (getConnectionType(context) == 1) {
                    if (info.isConnected) {
                        Logger.debug("Mobile connection detected, showing on mobile data dialog")
                        showOnMobileDataDialog()
                    } else {
                        Logger.debug("Mobile connection disabled, hiding on mobile data dialog")
                        hideOnMobileDataDialog()
                    }
                }

                // Restart the Ouinet client whenever connectivity has changed and become stable.
                val state = info.state
                if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.DISCONNECTED) {
                    OuinetService.stopOuinetService(this@BrowserActivity)
                    // TODO: Insert a pause / check client state.
                    OuinetService.startOuinetService(this@BrowserActivity, BrowserApplication.mOuinetConfig)
                }
            }
        }, intentFilter)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.container, createBrowserFragment(sessionId))
                commit()
            }
        }

        if (isCrashReportActive) {
            crashIntegration = CrashIntegration(this, components.analytics.crashReporter) { crash ->
                onNonFatalCrash(crash)
            }
            lifecycle.addObserver(crashIntegration)
        }

        /* Do not notify user of data policy because we are not collecting telemetry data
        *  and we already have a notification for stopping/pausing/purging local CENO data
        * NotificationManager.checkAndNotifyPolicy(this)
         */
        lifecycle.addObserver(webExtensionPopupFeature)
    }

    override fun onBackPressed() {
        supportFragmentManager.fragments.forEach {
            if (it is UserInteractionHandler && it.onBackPressed()) {
                return
            }
        }

        super.onBackPressed()

        removeSessionIfNeeded()
    }

    @Suppress("DEPRECATION") // ComponentActivity wants us to use registerForActivityResult
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Logger.info(
            "Activity onActivityResult received with " +
                "requestCode: $requestCode, resultCode: $resultCode, data: $data"
        )

        supportFragmentManager.fragments.forEach {
            if (it is ActivityResultHandler && it.onActivityResult(requestCode, data, resultCode)) {
                return
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * If needed remove the current session.
     *
     * If a session is a custom tab or was opened from an external app then the session gets removed once you go back
     * to the third-party app.
     *
     * Eventually we may want to move this functionality into one of our feature components.
     */
    private fun removeSessionIfNeeded(): Boolean {
        val session = tab ?: return false

        return if (session.source is SessionState.Source.External && !session.restored) {
            finish()
            components.useCases.tabsUseCases.removeTab(session.id)
            true
        } else {
            val hasParentSession = session is TabSessionState && session.parentId != null
            if (hasParentSession) {
                components.useCases.tabsUseCases.removeTab(session.id, selectParentIfExists = true)
            }
            // We want to return to home if this session didn't have a parent session to select.
            val goToOverview = !hasParentSession
            !goToOverview
        }
    }

    override fun onUserLeaveHint() {
        supportFragmentManager.fragments.forEach {
            if (it is UserInteractionHandler && it.onHomePressed()) {
                return
            }
        }

        super.onUserLeaveHint()
    }

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? =
        when (name) {
            EngineView::class.java.name -> components.core.engine.createView(context, attrs).asView()
            else -> super.onCreateView(parent, name, context, attrs)
        }

    private fun onNonFatalCrash(crash: Crash) {
        Snackbar.make(findViewById(android.R.id.content), R.string.crash_report_non_fatal_message, LENGTH_LONG)
            .setAction(R.string.crash_report_non_fatal_action) {
                crashIntegration.sendCrashReport(crash)
            }.show()
    }

    private fun openPopup(webExtensionState: WebExtensionState) {
        val intent = Intent(this, WebExtensionActionPopupActivity::class.java)
        intent.putExtra("web_extension_id", webExtensionState.id)
        intent.putExtra("web_extension_name", webExtensionState.name)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}
