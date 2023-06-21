/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
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
import ie.equalit.ceno.home.HomeFragment
import ie.equalit.ceno.browser.CrashIntegration
import ie.equalit.ceno.components.ceno.CenoWebExt.CENO_EXTENSION_ID
import ie.equalit.ceno.components.ceno.TopSitesStorageObserver
import ie.equalit.ceno.components.ceno.appstate.AppAction
import ie.equalit.ceno.ext.ceno.sort
import ie.equalit.ceno.ext.components
import ie.equalit.ceno.ext.isCrashReportActive
import ie.equalit.ceno.onboarding.OnboardingFragment
import ie.equalit.ceno.settings.Settings
import ie.equalit.ouinet.OuinetNotification
import ie.equalit.ceno.settings.SettingsFragment
import ie.equalit.ceno.browser.ShutdownFragment
import ie.equalit.ceno.components.PermissionHandler
import ie.equalit.ceno.ext.requireComponents
import ie.equalit.ceno.onboarding.OnboardingBatteryFragmentDirections
import mozilla.components.browser.state.selector.selectedTab
import ie.equalit.ceno.settings.AboutFragment
import mozilla.components.browser.state.state.*
import kotlin.system.exitProcess

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

    private val navHost by lazy {
        supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    }

    private var isActivityResumed = false
    private var lastCall: (() -> Unit)? = null

    /**
     * Returns a new instance of [BrowserFragment] to display.
     */
    open fun createBrowserFragment(sessionId: String?): Fragment =
        BrowserFragment.create(sessionId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        components.useCases.customLoadUrlUseCase.onNoSelectedTab = { url ->
            openToBrowser(url, newTab = true)
        }

        Logger.info(" --------- Starting ouinet service")
        components.ouinet.let {
            it.setOnNotificationTapped {
                beginShutdown(false)
            }
            it.setOnConfirmTapped {
                beginShutdown(true)
            }
            it.setBackground(this)
        }

        components.ouinet.background.startup()

        /* CENO: Set default behavior for AppBar */
        supportActionBar!!.apply {
            hide()
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.ceno_action_bar)))
        }

        val safeIntent = SafeIntent(intent)

        if (Settings.shouldShowOnboarding(this)) {
            if (savedInstanceState == null) {
                /* CENO: Choose which fragment to display first based on onboarding flag and selected tab */
                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.container, OnboardingFragment.create(sessionId), OnboardingFragment.TAG)
                    commit()
                }
                 */
                navHost.navController.navigate(NavGraphDirections.actionGlobalStartupOnboarding())
            }
        }
        else {
            if(savedInstanceState == null &&
                safeIntent.action != Intent.ACTION_VIEW) {
                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.container, HomeFragment.create(sessionId), HomeFragment.TAG)
                    commit()
                }
            }
            if (savedInstanceState != null ||
                safeIntent.action == Intent.ACTION_VIEW) {
                /* either there is a savedInstanceState or opened from a link */
                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.container, BrowserFragment.create(sessionId), BrowserFragment.TAG)
                    commit()
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

    override fun onPause() {
        super.onPause()
        isActivityResumed = false
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            /* CENO: in Android 9 or later, it is possible that the
             * service may have stopped while app was in background
             * try sending an intent to restart the service
             */
            Logger.info(" --------- Starting ouinet service onResume")
            components.ouinet.background.start()
        }
        isActivityResumed = true
        //If we have some fragment to show do it now then clear the queue
        if(lastCall != null){
            updateView(lastCall!!)
            lastCall = null
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
        supportFragmentManager.fragments.iterator().forEach {
            /* If coming from settings fragment, always clear back stack and go back to root fragment */
            if (it.tag == SettingsFragment.TAG) {
                if (components.core.store.state.selectedTabId == "" ||
                    components.core.store.state.selectedTabId == null
                        ) {
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.container, HomeFragment.create(sessionId), HomeFragment.TAG)
                        commit()
                    }
                }
                else {
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.container, BrowserFragment.create(sessionId), BrowserFragment.TAG)
                        commit()
                    }
                }
                return
            }
            if (it.tag == AboutFragment.TAG) {
                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.container, SettingsFragment(), SettingsFragment.TAG)
                    commit()
                }
                return
            }
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

        supportFragmentManager.fragments.iterator().forEach {
            if (it is ActivityResultHandler && it.onActivityResult(requestCode, data, resultCode)) {
                return
            }
        }

        if (requestCode == PermissionHandler.PERMISSION_CODE_IGNORE_BATTERY_OPTIMIZATIONS) {
            if (components.permissionHandler.onActivityResult(requestCode, data, resultCode)) {
                val action = OnboardingBatteryFragmentDirections
                    .actionOnboardingBatteryFragmentToOnboardingThanksFragment()
                navHost.navController.navigate(action)
            } else {
                updateView {
                    val action = OnboardingBatteryFragmentDirections
                        .actionOnboardingBatteryFragmentToOnboardingWarningFragment()
                    navHost.navController.navigate(action)
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    /* CENO: Handle intent sent to BrowserActivity to open to Homepage or open a homescreen shortcut link */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val safeIntent = intent?.let { SafeIntent(it) }
        if(safeIntent?.action == Intent.ACTION_MAIN &&
            safeIntent.hasExtra(OuinetNotification.FROM_NOTIFICATION_EXTRA)
        ){
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.container, HomeFragment.create(sessionId), HomeFragment.TAG)
                commit()
            }
        }
        if(safeIntent?.action == Intent.ACTION_VIEW) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.container, BrowserFragment.create(sessionId), BrowserFragment.TAG)
                commit()
            }
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
        supportFragmentManager.fragments.iterator().forEach {
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
    fun openToBrowser(url : String? = null, newTab : Boolean = false, private: Boolean = false){
        if (url != null) {
            if (newTab) {
                components.useCases.tabsUseCases.addTab(
                    url = url,
                    selectTab = true,
                    private = private,
                )
            } else {
                components.useCases.sessionUseCases.loadUrl(
                    url = url
                )
            }
        }
        showBrowser()
    }

    private fun showBrowser() {
        supportFragmentManager.findFragmentByTag(BrowserFragment.TAG)?.let {
            if (it.isVisible) {
                /* CENO: BrowserFragment is already being displayed, don't do another transaction */
                return
            }
        }
        try {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.container, BrowserFragment.create(sessionId), BrowserFragment.TAG)
                commit()
            }
        } catch (ex: Exception) {
            /* Workaround for opening shortcut from homescreen, try again allowing for state loss */
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.container, BrowserFragment.create(sessionId), BrowserFragment.TAG)
                commitAllowingStateLoss()
            }
        }
    }

    fun updateView(action: () -> Unit){
        //If the activity is in background we register the transaction
        if(!isActivityResumed){
            lastCall = action
        } else {
            //Else we just invoke it
            action.invoke()
        }
    }

    private fun shutdownCallback(doClear: Boolean) : Runnable {
        return Runnable {
            if (doClear) {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.clearApplicationUserData()
            }
            exitOuinetServiceProcess()
            exitProcess(0)
        }
    }

    fun beginShutdown(doClear : Boolean) {
        val handler = Handler(Looper.myLooper()!!)
        val callback = shutdownCallback(doClear)
        handler.postDelayed(
            callback,
            resources.getInteger(R.integer.shutdown_fragment_stalled_duration).toLong()
        )
        components.ouinet.background.shutdown(doClear) {
            handler.removeCallbacks(callback)
            callback.run()
        }
        updateView {
            ShutdownFragment.transitionToFragment(this, doClear)
        }
    }

    fun exitOuinetServiceProcess() {
        getSystemService(Context.ACTIVITY_SERVICE).let { am ->
            (am as ActivityManager).runningAppProcesses?.let { processes ->
                for (process in processes) {
                    if (process.processName.contains("ouinetService")){
                        Process.killProcess(process.pid)
                    }
                }
            }
        }
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
