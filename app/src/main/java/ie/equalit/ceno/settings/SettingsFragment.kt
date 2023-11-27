/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import ie.equalit.ceno.AppPermissionCodes
import ie.equalit.ceno.BrowserActivity
import ie.equalit.ceno.R
import ie.equalit.ceno.R.string.*
import ie.equalit.ceno.autofill.AutofillPreference
import ie.equalit.ceno.downloads.DownloadService
import ie.equalit.ceno.ext.*
import ie.equalit.ceno.utils.CenoPreferences
import ie.equalit.ceno.utils.sentry.SentryOptionsConfiguration
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.*
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.manager.FetchDownloadManager
import mozilla.components.support.base.feature.PermissionsFeature
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.view.showKeyboard
import org.mozilla.geckoview.BuildConfig
import kotlin.system.exitProcess

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var cenoPrefs: CenoPreferences
    private val downloadsFeature = ViewBoundFeatureWrapper<DownloadsFeature>()

    private lateinit var runnable: Runnable
    private var handler = Handler(Looper.getMainLooper())

    private val defaultClickListener = OnPreferenceClickListener { preference ->
        Toast.makeText(context, "${preference.title} Clicked", LENGTH_SHORT).show()
        true
    }

    private val sharedPreferencesChangeListener = OnSharedPreferenceChangeListener { sharedPrefs, key ->
        val newValue = sharedPrefs.getBoolean(key, false)
        if (key == getString(pref_key_shared_prefs_reload)) {
            Logger.debug("Got change listener for $key = $newValue")
            if (newValue) {
                Logger.debug("Reloading Settings fragment")
                CenoSettings.setStatusUpdateRequired(requireContext(), false)
                findNavController().popBackStack() // Pop before relaunching the fragment to preserve backstack
                findNavController().navigate(R.id.action_global_settings)
            }
        } else if (key == getString(pref_key_shared_prefs_update)) {
            if (newValue) {
                /* toggle preferences to refresh value */
                getPreference(pref_key_ouinet_state)?.let {
                    it.isEnabled = !(it.isEnabled)
                    it.isEnabled = !(it.isEnabled)
                }
                getPreference(pref_key_ceno_cache_size)?.let {
                    it.isEnabled = !(it.isEnabled)
                    it.isEnabled = !(it.isEnabled)
                }
                getPreference(pref_key_ceno_groups_count)?.let {
                    it.isEnabled = !(it.isEnabled)
                    it.isEnabled = !(it.isEnabled)
                }
                getPreference(pref_key_ceno_download_log)?.let {
                    it.isVisible = CenoSettings.isCenoLogEnabled(requireContext())
                    it.isEnabled = !(it.isEnabled)
                    it.isEnabled = !(it.isEnabled)
                }
                cenoPrefs.sharedPrefsUpdate = false
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        cenoPrefs = requireComponents.cenoPreferences
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /* TODO: The downloads feature is also used by the BaseBrowserFragment,
            should move this to a BaseFragment that both Settings and BaseBrowserFragment can inherit */
        downloadsFeature.set(
            feature = DownloadsFeature(
                requireContext(),
                store = requireComponents.core.store,
                useCases = requireComponents.useCases.downloadsUseCases,
                fragmentManager = childFragmentManager,
                downloadManager = FetchDownloadManager(
                    requireContext().applicationContext,
                    requireComponents.core.store,
                    DownloadService::class,
                    notificationsDelegate = requireComponents.notificationsDelegate
                ),
                onNeedToRequestPermissions = { permissions ->
                    // The Fragment class wants us to use registerForActivityResult
                    @Suppress("DEPRECATION")
                    requestPermissions(permissions, AppPermissionCodes.REQUEST_CODE_DOWNLOAD_PERMISSIONS)
                },
            ),
            owner = this,
            view = view,
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        val feature: PermissionsFeature? = when (requestCode) {
            AppPermissionCodes.REQUEST_CODE_DOWNLOAD_PERMISSIONS -> downloadsFeature.get()
            else -> null
        }
        feature?.onPermissionsResult(permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        cenoPrefs.preferences.registerOnSharedPreferenceChangeListener(sharedPreferencesChangeListener)
        setupPreferences()
        setupCenoSettings()
        getActionBar().apply {
            show()
            setTitle(settings)
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(requireContext(), R.color.ceno_action_bar)))
        }

        runnable = Runnable {
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    /* Fetch ouinet status without refreshing... */
                    CenoSettings.ouinetClientRequest(
                        requireContext(),
                        OuinetKey.API_STATUS,
                        ouinetResponseListener = object : OuinetResponseListener {
                            override fun onSuccess(message: String, data: Any?) {
                            }
                            override fun onError() {
                                CenoSettings.setOuinetState(requireContext(), "stopped")
                            }
                        },
                        shouldRefresh = false
                    )
                    withContext(Dispatchers.Main) {

                        /* Update summary text for Browser Service */
                        getPreference(pref_key_ouinet_state)?.summaryProvider = Preference.SummaryProvider<Preference> {
                            CenoSettings.getOuinetState(requireContext())
                        }

                        Logger.debug("Browser Service status updated via ${BROWSER_SERVICE_REFRESH_DELAY / 1000} second background refresh")
                        handler.postDelayed(runnable, BROWSER_SERVICE_REFRESH_DELAY)
                    }
                }
            }
        }

        handler.postDelayed(runnable, BROWSER_SERVICE_REFRESH_DELAY)
    }

    override fun onPause() {
        super.onPause()
        cenoPrefs.preferences.unregisterOnSharedPreferenceChangeListener(sharedPreferencesChangeListener)
        cenoPrefs.sharedPrefsReload = false
        handler.removeCallbacks(runnable)
    }

    private fun setupPreferences() {

        if (!AutofillPreference.isSupported(requireContext())) {
            getAutofillPreference(pref_key_autofill)?.isVisible = false
        } else {
            (getAutofillPreference(pref_key_autofill) as AutofillPreference).updateSwitch()
        }

        getPreference(pref_key_make_default_browser)?.onPreferenceClickListener = getClickListenerForMakeDefaultBrowser()
        getSwitchPreferenceCompat(pref_key_remote_debugging)?.onPreferenceChangeListener = getChangeListenerForRemoteDebugging()
        getPreference(pref_key_about_page)?.onPreferenceClickListener = getAboutPageListener()
        getPreference(pref_key_privacy)?.onPreferenceClickListener = getClickListenerForPrivacy()
        getPreference(pref_key_override_amo_collection)?.onPreferenceClickListener = getClickListenerForCustomAddons()
        getPreference(pref_key_customization)?.onPreferenceClickListener = getClickListenerForCustomization()
        getPreference(pref_key_delete_browsing_data)?.onPreferenceClickListener = getClickListenerForDeleteBrowsingData()
        getSwitchPreferenceCompat(pref_key_allow_crash_reporting)?.onPreferenceChangeListener = getClickListenerForCrashReporting()
        getPreference(pref_key_search_engine)?.onPreferenceClickListener = getClickListenerForSearch()
        getPreference(pref_key_add_ons)?.onPreferenceClickListener = getClickListenerForAddOns()
        getPreference(pref_key_ceno_website_sources)?.onPreferenceClickListener = getClickListenerForWebsiteSources()

        getPreference(pref_key_search_engine)?.summary = getString(setting_item_selected, requireContext().components.core.store.state.search.selectedOrDefaultSearchEngine?.name)

        // Update notifications
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !requireComponents.permissionHandler.isAllowingPostNotifications() -> {
                getPreference(pref_key_allow_notifications)?.isVisible = true
                getPreference(pref_key_allow_notifications)?.onPreferenceClickListener = getClickListenerForAllowNotifications()
            }

            else -> {
                getPreference(pref_key_allow_notifications)?.isVisible = false
            }
        }

        // Update battery optimization
        when {
            requireComponents.permissionHandler.isIgnoringBatteryOptimizations() -> {
                getPreference(pref_key_disable_battery_opt)?.isVisible = false
            }

            else -> {
                getPreference(pref_key_disable_battery_opt)?.isVisible = true
                getPreference(pref_key_disable_battery_opt)?.onPreferenceClickListener = getClickListenerForDisableBatteryOpt()
            }
        }

    }

    private fun setPreference(
        pref: Preference?,
        enabled: Boolean,
        changeListener: OnPreferenceChangeListener? = null,
        clickListener: OnPreferenceClickListener? = null
    ) {
        pref?.let {
            it.isEnabled = enabled
            it.shouldDisableView = !enabled
            it.onPreferenceChangeListener = changeListener
            it.onPreferenceClickListener = clickListener
        }
    }

    private fun setupCenoSettings() {
        getPreference(pref_key_ceno_download_log)?.isVisible = CenoSettings.isCenoLogEnabled(requireContext())
        getPreference(pref_key_about_ceno)?.summary = CenoSettings.getCenoVersionString(requireContext())
        getPreference(pref_key_about_geckoview)?.summary = BuildConfig.MOZ_APP_VERSION + "-" + BuildConfig.MOZ_APP_BUILDID

        if (CenoSettings.isStatusUpdateRequired(requireContext())) {
            /* Ouinet status not yet updated */
            /* Grey out all Ceno related options */
            setPreference(getPreference(pref_key_ceno_groups_count), false)
            setPreference(getPreference(pref_key_clear_ceno_cache), false)
            setPreference(getPreference(pref_key_ceno_network_config), false)
            setPreference(getPreference(pref_key_ceno_enable_log), false)
            setPreference(getPreference(pref_key_ceno_download_log), false)
            /* Fetch ouinet status */
            CenoSettings.ouinetClientRequest(requireContext(), OuinetKey.API_STATUS)
        } else {
            /* Enable Ceno related options */
            getPreference(pref_key_ouinet_state)?.summaryProvider = Preference.SummaryProvider<Preference> {
                CenoSettings.getOuinetState(requireContext())
            }
            getPreference(pref_key_ceno_cache_size)?.summaryProvider = Preference.SummaryProvider<Preference> {
                CenoSettings.getCenoCacheSize(requireContext())
            }
            CenoSettings.ouinetClientRequest(requireContext(), OuinetKey.GROUPS_TXT)
            getPreference(pref_key_ceno_groups_count)?.summaryProvider = Preference.SummaryProvider<Preference> {
                String.format("%d sites", CenoSettings.getCenoGroupsCount(requireContext()))
            }
            setPreference(
                getPreference(pref_key_ceno_groups_count),
                true,
                clickListener = getClickListenerForCenoGroupsCounts()
            )
            setPreference(
                getPreference(pref_key_clear_ceno_cache),
                true,
                clickListener = getClickListenerForClearCenoCache()
            )
            setPreference(
                getPreference(pref_key_ceno_network_config),
                true,
                clickListener = getClickListenerForCenoNetworkDetails()
            )
            setPreference(
                getPreference(pref_key_ceno_enable_log),
                true,
                changeListener = getChangeListenerForCenoSetting(OuinetKey.LOGFILE)
            )
            setPreference(
                getPreference(pref_key_ceno_download_log),
                true,
                clickListener = getClickListenerForCenoDownloadLog()
            )
            getPreference(pref_key_about_ouinet)?.summary = CenoSettings.getOuinetVersion(requireContext()) + " " +
                CenoSettings.getOuinetBuildId(requireContext())
        }
    }


    private fun getClickListenerForMakeDefaultBrowser(): OnPreferenceClickListener {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            OnPreferenceClickListener {
                val intent = Intent(
                    Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
                )
                startActivity(intent)
                true
            }
        } else {
            defaultClickListener
        }
    }

    private fun getClickListenerForPrivacy(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_privacySettingsFragment
            )
            getActionBar().setTitle(tracker_category)
            true
        }
    }

    private fun getClickListenerForCustomization(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_customizationSettingsFragment
            )
            true
        }
    }

    private fun getClickListenerForSearch(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_installedSearchEnginesSettingsFragment
            )
            getActionBar().setTitle(preference_choose_search_engine)
            true
        }
    }

    private fun getClickListenerForDeleteBrowsingData(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_deleteBrowsingDataFragment
            )
            getActionBar().setTitle(preferences_delete_browsing_data)
            true
        }
    }

    private fun getClickListenerForAddOns(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            findNavController().navigate(R.id.action_global_addons)
            true
        }
    }

    private fun getClickListenerForWebsiteSources(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_websiteSourceSettingsFragment)
            true
        }
    }

    private fun getClickListenerForCrashReporting(): OnPreferenceChangeListener {
        return OnPreferenceChangeListener { _, _ ->
            // Re-initialize Sentry-Android
            SentryAndroid.init(
                requireContext(),
                SentryOptionsConfiguration.getConfig(requireContext())
            )

//            Re-allow post-crash permissions nudge
//            This should ALWAYS be turned on when this permission state is toggled
            ie.equalit.ceno.settings.Settings.toggleCrashReportingPermissionNudge(requireContext(), true)
            true
        }
    }

    private fun getChangeListenerForRemoteDebugging(): OnPreferenceChangeListener {
        return OnPreferenceChangeListener { _, newValue ->
            requireComponents.core.engine.settings.remoteDebuggingEnabled = newValue as Boolean
            true
        }
    }

    private fun getAboutPageListener(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_aboutFragment
            )
            getActionBar().setTitle(preferences_about_page)
            true
        }
    }

    private fun getActionBar() = (activity as AppCompatActivity).supportActionBar!!

    private fun getClickListenerForCustomAddons(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            val context = requireContext()
            val dialogView = View.inflate(context, R.layout.amo_collection_override_dialog, null)
            val userView = dialogView.findViewById<EditText>(R.id.custom_amo_user)
            val collectionView = dialogView.findViewById<EditText>(R.id.custom_amo_collection)

            AlertDialog.Builder(context).apply {
                setTitle(context.getString(preferences_customize_amo_collection))
                setView(dialogView)
                setNegativeButton(customize_addon_collection_cancel) { dialog: DialogInterface, _ ->
                    dialog.cancel()
                }

                setPositiveButton(customize_addon_collection_ok) { _, _ ->
                    ie.equalit.ceno.settings.Settings.setOverrideAmoUser(
                        context,
                        userView.text.toString()
                    )
                    ie.equalit.ceno.settings.Settings.setOverrideAmoCollection(
                        context,
                        collectionView.text.toString()
                    )

                    Toast.makeText(
                        context,
                        getString(toast_customize_addon_collection_done),
                        Toast.LENGTH_LONG,
                    ).show()

                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            exitProcess(0)
                        },
                        AMO_COLLECTION_OVERRIDE_EXIT_DELAY,
                    )
                }

                collectionView.setText(
                    ie.equalit.ceno.settings.Settings.getOverrideAmoCollection(
                        context
                    )
                )
                userView.setText(ie.equalit.ceno.settings.Settings.getOverrideAmoUser(context))
                userView.requestFocus()
                userView.showKeyboard()
                create()
            }.show()
            true
        }
    }

    /*
    private fun getChangeListenerForMobileData(): OnPreferenceChangeListener {
        return OnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                /* this should pop-up the mobile data dialog to provide warning and allow user to select option*/
                Toast.makeText(context, preferences_mobile_data_warning_disabled, LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(context, preferences_mobile_data_warning_enabled, LENGTH_SHORT).show()
            }
            true
        }
    }
     */

    private fun getClickListenerForAllowNotifications(): OnPreferenceClickListener {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OnPreferenceClickListener {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().packageName)
                    requireActivity().startActivity(this)
                }
                true
            }
        } else {
            defaultClickListener
        }
    }

    private fun getClickListenerForDisableBatteryOpt(): OnPreferenceClickListener {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            OnPreferenceClickListener {
                requireComponents.permissionHandler.requestBatteryOptimizationsOff(requireActivity())
                true
            }
        } else {
            defaultClickListener
        }
    }

    private fun getChangeListenerForCenoSetting(key: OuinetKey): OnPreferenceChangeListener {
        return OnPreferenceChangeListener { _, newValue ->
            val value = if (newValue == true) {
                OuinetValue.ENABLED
            } else {
                OuinetValue.DISABLED
            }
            CenoSettings.ouinetClientRequest(requireContext(), key, value)
            true
        }
    }

    private fun getClickListenerForClearCenoCache(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            CenoSettings.ouinetClientRequest(requireContext(), OuinetKey.PURGE_CACHE)
            //ClearButtonFeature.createClearDialog(requireContext()).show()
            true
        }
    }

    private fun getClickListenerForCenoGroupsCounts(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            (activity as BrowserActivity).openToBrowser(
                "${CenoSettings.SET_VALUE_ENDPOINT}/${OuinetKey.GROUPS_TXT.command}",
                newTab = true
            )
            true
        }
    }

    private fun getClickListenerForCenoNetworkDetails(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            findNavController().navigate(
                R.id.action_settingsFragment_to_networkSettingsFragment
            )
            true
        }
    }

    private fun getClickListenerForCenoDownloadLog(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            val store = requireComponents.core.store
            val logUrl = "${CenoSettings.SET_VALUE_ENDPOINT}/${CenoSettings.LOGFILE_TXT}"
            val download = DownloadState(logUrl)
            createTab(logUrl).apply {
                store.dispatch(TabListAction.AddTabAction(this, select = true))
                store.dispatch(ContentAction.UpdateDownloadAction(this.id, download))
            }
            (activity as BrowserActivity).openToBrowser()
            true
        }
    }

    companion object {
        private const val AMO_COLLECTION_OVERRIDE_EXIT_DELAY = 3000L
        private const val BROWSER_SERVICE_REFRESH_DELAY = 5000L
    }
}
