/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import ie.equalit.ceno.components.ceno.CenoLocationUtils
import ie.equalit.ceno.ext.application
import ie.equalit.ceno.ext.isCrashReportActive
import ie.equalit.ceno.settings.Settings
import ie.equalit.ouinet.Config
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.SystemAction
import mozilla.components.concept.engine.webextension.isUnsupported
import mozilla.components.feature.addons.update.GlobalAddonDependencyProvider
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.sink.AndroidLogSink
import mozilla.components.support.ktx.android.content.isMainProcess
import mozilla.components.support.ktx.android.content.runOnlyInMainProcess
import mozilla.components.support.rusthttp.RustHttpConfig
import mozilla.components.support.rustlog.RustLog
import mozilla.components.support.webextensions.WebExtensionSupport
import java.util.*
import java.util.concurrent.TimeUnit

open class BrowserApplication : Application() {
    val components by lazy { Components(this) }

    override fun onCreate() {
        super.onCreate()

        /* CENO: Read default preferences and set the default theme immediately at startup */
        PreferenceManager.setDefaultValues(this, R.xml.default_preferences, false)
        AppCompatDelegate.setDefaultNightMode(
                Settings.getAppTheme(this)
        )

        setupCrashReporting(this)

        RustHttpConfig.setClient(lazy { components.core.client })
        setupLogging()

        //------------------------------------------------------------
        // Ouinet
        //------------------------------------------------------------

        var btBootstrapExtras: Set<String>? = null

        var countryIsoCode = ""
        val locationUtils = CenoLocationUtils(application)
        countryIsoCode = locationUtils.currentCountry

        // Attempt getting country-specific `BT_BOOTSTRAP_EXTRAS` entry from BuildConfig,
        // fall back to empty BT bootstrap extras otherwise.
        var btbsxsStr= ""
        if (countryIsoCode.isNotEmpty()) {
            // Country code found, try getting bootstrap extras resource for this country
            for (entry in BuildConfig.BT_BOOTSTRAP_EXTRAS) {
                if (countryIsoCode == entry[0]) {
                    btbsxsStr = entry[1]
                }
            }
        }

        if (btbsxsStr != "") {
            // Bootstrap extras resource found
            val btbsxs: HashSet<String> = HashSet()
            for (x in btbsxsStr.split(" ").toTypedArray()) {
                if (x.isNotEmpty()) {
                    btbsxs.add(x)
                }
            }
            if (btbsxs.size > 0) {
                btBootstrapExtras = btbsxs
            }
        }
        // else no bootstrap extras included, leave null

        mOuinetConfig = Config.ConfigBuilder(this)
                .setCacheHttpPubKey(BuildConfig.CACHE_PUB_KEY)
                .setInjectorCredentials(BuildConfig.INJECTOR_CREDENTIALS)
                .setInjectorTlsCert(BuildConfig.INJECTOR_TLS_CERT)
                .setTlsCaCertStorePath("file:///android_asset/cacert.pem")
                .setCacheType("bep5-http")
                .setLogLevel(Config.LogLevel.DEBUG)
                .setBtBootstrapExtras(btBootstrapExtras)
                .setListenOnTcp("127.0.0.1:${BuildConfig.PROXY_PORT}")
                .setFrontEndEp("127.0.0.1:${BuildConfig.FRONTEND_PORT}")
                .build()

        //------------------------------------------------------------


        if (!isMainProcess()) {
            // If this is not the main process then do not continue with the initialization here. Everything that
            // follows only needs to be done in our app's main process and should not be done in other processes like
            // a GeckoView child process or the crash handling process. Most importantly we never want to end up in a
            // situation where we create a GeckoRuntime from the Gecko child process (
            return
        }

        /* CENO: Must add root cert prior to startup of Gecko Engine, so it is installed during GeckoViewStartup */
        components.core.setRootCertificate(mOuinetConfig!!.caRootCertPath)

        components.core.engine.warmUp()

        restoreBrowserState()

        GlobalAddonDependencyProvider.initialize(
            components.core.addonManager,
            components.core.addonUpdater,
        )
        WebExtensionSupport.initialize(
            runtime = components.core.engine,
            store = components.core.store,
            onNewTabOverride = { _, engineSession, url ->
                val tabId = components.useCases.tabsUseCases.addTab(
                    url = url,
                    selectTab = true,
                    engineSession = engineSession,
                )
                tabId
            },
            onCloseTabOverride = { _, sessionId ->
                components.useCases.tabsUseCases.removeTab(sessionId)
            },
            onSelectTabOverride = { _, sessionId ->
                components.useCases.tabsUseCases.selectTab(sessionId)
            },
            onExtensionsLoaded = { extensions ->
                components.core.addonUpdater.registerForFutureUpdates(extensions)

                val checker = components.core.supportedAddonsChecker
                val hasUnsupportedAddons = extensions.any { it.isUnsupported() }
                if (hasUnsupportedAddons) {
                    checker.registerForChecks()
                } else {
                    // As checks are a persistent subscriptions, we have to make sure
                    // we remove any previous subscriptions.
                    checker.unregisterForChecks()
                }
            },
            onUpdatePermissionRequest = components.core.addonUpdater::onUpdatePermissionRequest,
        )

        /* CENO F-Droid: Not using firefox accounts, firebase push breaks f-droid build */
        /*
        components.push.feature?.let {
            Logger.info("AutoPushFeature is configured, initializing it...")

            PushProcessor.install(it)

            // WebPush integration to observe and deliver push messages to engine.
            WebPushEngineIntegration(components.core.engine, it).start()

            // Perform a one-time initialization of the account manager if a message is received.
            PushFxaIntegration(it, lazy { components.backgroundServices.accountManager }).launch()

            // Initialize the push feature and service.
            it.initialize()
        }
        */
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        runOnlyInMainProcess {
            components.core.store.dispatch(SystemAction.LowMemoryAction(level))
            components.core.icons.onTrimMemory(level)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun restoreBrowserState() = GlobalScope.launch(Dispatchers.Main) {
        val store = components.core.store
        val sessionStorage = components.core.sessionStorage

        components.useCases.tabsUseCases.restore(sessionStorage)

        // Now that we have restored our previous state (if there's one) let's setup auto saving the state while
        // the app is used.
        sessionStorage.autoSave(store)
            .periodicallyInForeground(interval = 30, unit = TimeUnit.SECONDS)
            .whenGoingToBackground()
            .whenSessionsChange()
    }

    companion object {
        var mOuinetConfig: Config? = null
        const val NON_FATAL_CRASH_BROADCAST = "ie.equalit.ceno"
        init {
            System.setProperty("http.proxyHost", "127.0.0.1")
            System.setProperty("http.proxyPort", BuildConfig.PROXY_PORT)

            System.setProperty("https.proxyHost", "127.0.0.1")
            System.setProperty("https.proxyPort", BuildConfig.PROXY_PORT)
        }
    }
}

private fun setupLogging() {
    // We want the log messages of all builds to go to Android logcat
    Log.addSink(AndroidLogSink())
    RustLog.enable()
}

private fun setupCrashReporting(application: BrowserApplication) {
    if (isCrashReportActive) {
        application
            .components
            .analytics
            .crashReporter.install(application)
    }
}
