/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
import org.mozilla.reference.browser.browser.CenoHomeFragment
import org.mozilla.reference.browser.browser.CrashIntegration
import org.mozilla.reference.browser.components.ceno.MobileDataDialog
import org.mozilla.reference.browser.components.ceno.OuinetService
import org.mozilla.reference.browser.components.ceno.TopSitesStorageObserver
import org.mozilla.reference.browser.components.ceno.appstate.AppAction
import org.mozilla.reference.browser.ext.ceno.sort
import org.mozilla.reference.browser.ext.components
import org.mozilla.reference.browser.ext.isCrashReportActive

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

    /**
     * CENO: Returns a new instance of [CenoHomeFragment] to display.
     */
    open fun createCenoHomeFragment(sessionId: String?): Fragment =
        CenoHomeFragment.create(sessionId)

    /**
     * Returns a new instance of [BrowserFragment] to display.
     */
    open fun createBrowserFragment(sessionId: String?): Fragment =
        BrowserFragment.create(sessionId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* CENO: Create service object that observes changes to mobile data status */
        MobileDataDialog(this, this)

        if (savedInstanceState == null) {
            /* CENO: Select or create an "about:home" tab on first start, then open CenoHomeFragment */
            components.useCases.tabsUseCases.selectOrAddTab(CenoHomeFragment.ABOUT_HOME)
            supportFragmentManager.beginTransaction().apply {
                /* CENO: Create HomeFragement when starting BrowserActivity instead of BrowserFragment */
                replace(R.id.container, createCenoHomeFragment(sessionId), CenoHomeFragment.TAG)
                commit()
            }
        }

        /* CENO: need to initialize top sites to be displayed in CenoHomeFragment */
        initializeTopSites()

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

    /* CENO: Handle intent sent to BrowserActivity to open tab if needed */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val uri = intent?.getStringExtra(OuinetService.URI_EXTRA)
        if (uri != null){
            components.useCases.tabsUseCases.selectOrAddTab(uri)
        }
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

    /* CENO: Add function to open requested site in BrowserFragment */
    fun openToBrowser(url : String, newTab : Boolean = false, private: Boolean = false){
        if (newTab) {
            components.useCases.tabsUseCases.addTab(
                url = url,
                selectTab = true,
                private = private,
            )
        }
        else {
            components.useCases.sessionUseCases.loadUrl(
                url = url
            )
        }
        /* No need to change fragments, this is handled by the toolbar observing the change of url */
    }

    /* CENO: Function to initialize top site storage and observer */
    @OptIn(DelicateCoroutinesApi::class)
    private fun initializeTopSites() {
        /*  Launch a coroutine to initialize top site storage cache and update it in the store */
        GlobalScope.launch(Dispatchers.IO) {
            components.core.cenoTopSitesStorage.getTopSites(
                totalSites = components.cenoPreferences.topSitesMaxLimit
            )
            components.appStore.dispatch(
                AppAction.Change(topSites = components.core.cenoTopSitesStorage.cachedTopSites.sort())
            )
        }

        /* Register TopSitesStorageObserver, which will update AppStore when top sites are changed/added/removed */
        components.core.cenoTopSitesStorage.apply{
            register(
                observer = TopSitesStorageObserver(
                    this,
                    components.cenoPreferences,
                    components.appStore)
            )
        }
    }
}
