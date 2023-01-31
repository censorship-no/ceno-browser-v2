/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.MenuItem
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
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.intent.ext.EXTRA_SESSION_ID
import mozilla.components.lib.crash.Crash
import mozilla.components.support.base.feature.ActivityResultHandler
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.webextensions.WebExtensionPopupFeature
import ie.equalit.ceno.addons.WebExtensionActionPopupActivity
import ie.equalit.ceno.browser.BrowserFragment
import ie.equalit.ceno.browser.CenoHomeFragment
import ie.equalit.ceno.browser.CrashIntegration
import ie.equalit.ceno.components.ceno.CenoWebExt.CENO_EXTENSION_ID
import ie.equalit.ceno.components.ceno.ConnectivityBroadcastReceiver
import ie.equalit.ceno.components.ceno.OuinetService
import ie.equalit.ceno.components.ceno.TopSitesStorageObserver
import ie.equalit.ceno.components.ceno.appstate.AppAction
import ie.equalit.ceno.ext.ceno.sort
import ie.equalit.ceno.ext.components
import ie.equalit.ceno.ext.isCrashReportActive
import ie.equalit.ceno.onboarding.OnboardingFragment
import ie.equalit.ceno.settings.Settings
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.*

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
    open fun createOnboardingFragment(sessionId: String?): Fragment =
        OnboardingFragment.create(sessionId)

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

        Logger.info(" --------- Starting ouinet service")
        OuinetService.startOuinetService(this, BrowserApplication.mOuinetConfig)

        /* CENO: Register receiver that receives intents on connectivity changes */
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        this.registerReceiver(ConnectivityBroadcastReceiver, intentFilter)
        if (savedInstanceState == null) {
            /* CENO: Set default behavior for AppBar */
            supportActionBar!!.apply {
                hide()
                setDisplayHomeAsUpEnabled(true)
                setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.ceno_action_bar)))
            }

            /* CENO: Choose which fragment to display first based on onboarding flag and selected tab */
            if (Settings.shouldShowOnboarding(this)) {
                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.container, createOnboardingFragment(sessionId), OnboardingFragment.TAG)
                    commit()
                }
            }
            else {
                if (components.core.store.state.selectedTab?.content?.url == CenoHomeFragment.ABOUT_HOME) {
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.container, createCenoHomeFragment(sessionId), CenoHomeFragment.TAG)
                        commit()
                    }
                }
                else {
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.container, createBrowserFragment(sessionId), BrowserFragment.TAG)
                        commit()
                    }
                }
            }
        }

        /* CENO: need to initialize top sites to be displayed in CenoHomeFragment */
        initializeTopSites()

        initializeSearchEngines()

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

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            /* CENO: in Android 9 or later, it is possible that the
             * service may have stopped while app was in background
             * try sending an intent to restart the service
             */
            Logger.info(" --------- Starting ouinet service onResume")
            OuinetService.startOuinetService(this, BrowserApplication.mOuinetConfig)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
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

    /* CENO: Handle intent sent to BrowserActivity to open tab if needed or close the app */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val uri = intent?.getStringExtra(OuinetService.URI_EXTRA)
        if (uri != null){
            components.useCases.tabsUseCases.selectOrAddTab(uri)
        }
        val close = intent?.getBooleanExtra(OuinetService.CLOSE_EXTRA,false)
        if (close == true){
            this.finishAffinity()
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
        Snackbar.make(findViewById(android.R.id.content),
            R.string.crash_report_non_fatal_message, LENGTH_LONG)
            .setAction(R.string.crash_report_non_fatal_action) {
                crashIntegration.sendCrashReport(crash)
            }.show()
    }

    private fun openPopup(webExtensionState: WebExtensionState) {
        if (webExtensionState.id == CENO_EXTENSION_ID) {
            val fragment = supportFragmentManager.findFragmentByTag(BrowserFragment.TAG) as BrowserFragment
            fragment.showWebExtensionPopupPanel(webExtensionState.id)
        }
        else {
            val intent = Intent(this, WebExtensionActionPopupActivity::class.java)
            intent.putExtra("web_extension_id", webExtensionState.id)
            intent.putExtra("web_extension_name", webExtensionState.name)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
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
                AppAction.Change(
                    topSites = components.core.cenoTopSitesStorage.cachedTopSites.sort(),
                    showCenoModeItem = components.cenoPreferences.showCenoModeItem
                )
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

    private fun initializeSearchEngines() {
        if (Settings.shouldUpdateSearchEngines(this)) {
            components.core.store.state.search.searchEngines.filter { searchEngine ->
                searchEngine.id in listOf(
                        getString(R.string.remove_search_engine_id_1),
                        getString(R.string.remove_search_engine_id_2))
            }.forEach { searchEngine ->
                components.useCases.searchUseCases.removeSearchEngine(searchEngine)
            }
            components.core.store.state.search.searchEngines.forEach { searchEngine ->
                if (searchEngine.id == getString(R.string.default_search_engine_id)) {
                    components.useCases.searchUseCases.selectSearchEngine(searchEngine)
                }
            }
            Logger.debug("${components.core.store.state.search.searchEngines}")
            Logger.debug("${components.core.store.state.search.selectedOrDefaultSearchEngine}")
            Settings.setUpdateSearchEngines(this, false)
        }
    }
}
