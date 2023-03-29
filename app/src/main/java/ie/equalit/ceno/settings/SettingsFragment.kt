/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import ie.equalit.ceno.AppPermissionCodes
import ie.equalit.ceno.BrowserActivity
import ie.equalit.ceno.R
import ie.equalit.ceno.R.string.*
import ie.equalit.ceno.autofill.AutofillPreference
import ie.equalit.ceno.components.ceno.CenoWebExt
import ie.equalit.ceno.components.ceno.PermissionHandler
import ie.equalit.ceno.components.ceno.WebExtensionToolbarFeature
import ie.equalit.ceno.downloads.DownloadService
import ie.equalit.ceno.ext.components
import ie.equalit.ceno.ext.getPreferenceKey
import ie.equalit.ceno.ext.requireComponents
import ie.equalit.ceno.settings.deletebrowsingdata.DeleteBrowsingDataFragment
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.view.showKeyboard
import kotlin.system.exitProcess
import ie.equalit.ceno.utils.CenoPreferences
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.manager.FetchDownloadManager
import mozilla.components.support.base.feature.PermissionsFeature
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.geckoview.BuildConfig

@Suppress("TooManyFunctions")
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var cenoPrefs : CenoPreferences
    private val downloadsFeature = ViewBoundFeatureWrapper<DownloadsFeature>()

    protected val sessionId: String?
        get() = arguments?.getString(SESSION_ID)

    interface ActionBarUpdater {
        fun updateTitle(titleResId: Int)
    }

    private val defaultClickListener = OnPreferenceClickListener { preference ->
        Toast.makeText(context, "${preference.title} Clicked", LENGTH_SHORT).show()
        true
    }

    private val sharedPreferencesChangeListener = OnSharedPreferenceChangeListener {
            sharedPrefs, key ->
        val  newValue = sharedPrefs.getBoolean(key, false)
        if (key == getString(pref_key_shared_prefs_reload)) {
            Logger.debug("Got change listener for $key = $newValue")
            if (newValue) {
                Logger.debug("Reloading Settings fragment")
                CenoSettings.setStatusUpdateRequired(requireContext(), false)
                parentFragmentManager.beginTransaction().apply {
                    replace(R.id.container,
                        create(sessionId),
                        TAG
                    )
                    addToBackStack(null)
                    commit()
                }
            }
        }
        else if (key == getString(pref_key_shared_prefs_update)) {
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
        getActionBar().apply{
            show()
            setTitle(R.string.settings)
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.ceno_action_bar)))
        }
    }

    override fun onPause() {
        super.onPause()
        cenoPrefs.preferences.unregisterOnSharedPreferenceChangeListener(sharedPreferencesChangeListener)
        cenoPrefs.sharedPrefsReload = false
    }

    @Suppress("LongMethod") // Yep, this should be refactored.
    private fun setupPreferences() {
        val makeDefaultBrowserKey = requireContext().getPreferenceKey(pref_key_make_default_browser)
        val remoteDebuggingKey = requireContext().getPreferenceKey(pref_key_remote_debugging)
        val aboutPageKey = requireContext().getPreferenceKey(pref_key_about_page)
        val privacyKey = requireContext().getPreferenceKey(pref_key_privacy)
        val customAddonsKey = requireContext().getPreferenceKey(pref_key_override_amo_collection)
        val autofillPreferenceKey = requireContext().getPreferenceKey(R.string.pref_key_autofill)
        val disableBatteryOptKey = requireContext().getPreferenceKey(R.string.pref_key_disable_battery_opt)
        val customizationKey = requireContext().getPreferenceKey(pref_key_customization)
        val deleteBrowsingDataKey = requireContext().getPreferenceKey(pref_key_delete_browsing_data)
        val searchKey = requireContext().getPreferenceKey(pref_key_search_engine)

        val preferenceMakeDefaultBrowser = findPreference<Preference>(makeDefaultBrowserKey)
        val preferenceRemoteDebugging = findPreference<SwitchPreferenceCompat>(remoteDebuggingKey)
        val preferenceAboutPage = findPreference<Preference>(aboutPageKey)
        val preferencePrivacy = findPreference<Preference>(privacyKey)
        val preferenceCustomAddons = findPreference<Preference>(customAddonsKey)
        val preferenceAutofill = findPreference<AutofillPreference>(autofillPreferenceKey)
        val preferenceDisableBatteryOpt = findPreference<Preference>(disableBatteryOptKey)
        val preferenceCustomization = findPreference<Preference>(customizationKey)
        val preferenceDeleteBrowsingData = findPreference<Preference>(deleteBrowsingDataKey)
        val preferenceSearch = findPreference<Preference>(searchKey)

        if (!AutofillPreference.isSupported(requireContext())) {
            preferenceAutofill?.isVisible = false
        } else {
            (preferenceAutofill as AutofillPreference).updateSwitch()
        }

        preferenceMakeDefaultBrowser?.onPreferenceClickListener = getClickListenerForMakeDefaultBrowser()
        preferenceRemoteDebugging?.onPreferenceChangeListener = getChangeListenerForRemoteDebugging()
        preferenceAboutPage?.onPreferenceClickListener = getAboutPageListener()
        preferencePrivacy?.onPreferenceClickListener = getClickListenerForPrivacy()
        preferenceCustomAddons?.onPreferenceClickListener = getClickListenerForCustomAddons()
        preferenceCustomization?.onPreferenceClickListener = getClickListenerForCustomization()
        preferenceDeleteBrowsingData?.onPreferenceClickListener = getClickListenerForDeleteBrowsingData()
        preferenceSearch?.onPreferenceClickListener = getClickListenerForSearch()
        if (PermissionHandler(requireContext()).isIgnoringBatteryOptimizations()) {
                preferenceDisableBatteryOpt?.isVisible = false
        }
        else {
            preferenceDisableBatteryOpt?.onPreferenceClickListener = getClickListenerForDisableBatteryOpt()
        }
    }

    private fun getPreference(key : Int) : Preference? {
        val prefKey = requireContext().getPreferenceKey(key)
        return findPreference(prefKey)
    }

    private fun setPreference(
        pref : Preference?,
        enabled : Boolean,
        changeListener: OnPreferenceChangeListener? = null,
        clickListener: OnPreferenceClickListener? = null
    ) {
        pref?.let{
            it.isEnabled = enabled
            it.shouldDisableView = !enabled
            it.onPreferenceChangeListener = changeListener
            it.onPreferenceClickListener = clickListener
        }
    }

    private fun setupCenoSettings() {
        val preferenceOuinetState = getPreference(pref_key_ouinet_state)
        val preferenceCenoSourcesOrigin = getPreference(pref_key_ceno_sources_origin)
        val preferenceCenoSourcesPrivate = getPreference(pref_key_ceno_sources_private)
        val preferenceCenoSourcesPublic = getPreference(pref_key_ceno_sources_public)
        val preferenceCenoSourcesShared = getPreference(pref_key_ceno_sources_shared)
        val preferenceCenoCacheSize = getPreference(pref_key_ceno_cache_size)
        val preferenceCenoGroupsCount = getPreference(pref_key_ceno_groups_count)
        val preferenceClearCenoCache = getPreference(pref_key_clear_ceno_cache)
        val preferenceCenoNetworkDetails = getPreference(pref_key_ceno_network_config)
        val preferenceCenoEnableLog = getPreference(pref_key_ceno_enable_log)
        val preferenceCenoDownloadLog = getPreference(pref_key_ceno_download_log)

        val preferenceAboutCeno = getPreference(pref_key_about_ceno)
        val preferenceAboutGeckview = getPreference(pref_key_about_geckoview)
        val preferenceAboutOuinet = getPreference(pref_key_about_ouinet)
        val preferenceAboutOuinetProtocol = getPreference(pref_key_about_ouinet_protocol)

        preferenceCenoDownloadLog?.isVisible = CenoSettings.isCenoLogEnabled(requireContext())
        preferenceAboutCeno?.summary =  CenoSettings.getCenoVersionString(requireContext())
        preferenceAboutGeckview?.summary = BuildConfig.MOZ_APP_VERSION + "-" + BuildConfig.MOZ_APP_BUILDID

        if (CenoSettings.isStatusUpdateRequired(requireContext())) {
            /* Ouinet status not yet updated */
            /* Grey out all Ceno related options */
            setPreference(preferenceCenoSourcesOrigin, false)
            setPreference(preferenceCenoSourcesPrivate, false)
            setPreference(preferenceCenoSourcesPublic, false)
            setPreference(preferenceCenoSourcesShared, false)
            setPreference(preferenceCenoGroupsCount, false)
            setPreference(preferenceClearCenoCache, false)
            setPreference(preferenceCenoNetworkDetails, false)
            setPreference(preferenceCenoEnableLog, false)
            setPreference(preferenceCenoDownloadLog, false)
            /* Fetch ouinet status */
            CenoSettings.ouinetClientRequest(requireContext(), OuinetKey.API_STATUS)
        }
        else {
            /* Enable Ceno related options */
            setPreference(
                preferenceCenoSourcesOrigin,
                true,
                changeListener = getChangeListenerForCenoSetting(OuinetKey.ORIGIN_ACCESS)
            )
            setPreference(
                preferenceCenoSourcesPrivate,
                true,
                changeListener = getChangeListenerForCenoSetting(OuinetKey.PROXY_ACCESS)
            )
            setPreference(
                preferenceCenoSourcesPublic,
                true,
                changeListener = getChangeListenerForCenoSetting(OuinetKey.INJECTOR_ACCESS)
            )
            setPreference(
                preferenceCenoSourcesShared,
                true,
                changeListener = getChangeListenerForCenoSetting(OuinetKey.DISTRIBUTED_CACHE)
            )
            preferenceOuinetState?.summaryProvider = Preference.SummaryProvider<Preference> {
                CenoSettings.getOuinetState(requireContext())
            }
            preferenceCenoCacheSize?.summaryProvider = Preference.SummaryProvider<Preference> {
                CenoSettings.getCenoCacheSize(requireContext())
            }
            CenoSettings.ouinetClientRequest(requireContext(), OuinetKey.GROUPS_TXT)
            preferenceCenoGroupsCount?.summaryProvider = Preference.SummaryProvider<Preference> {
                String.format( "%d sites", CenoSettings.getCenoGroupsCount(requireContext()))
            }
            setPreference(
                preferenceCenoGroupsCount,
                true,
                clickListener = getClickListenerForCenoGroupsCounts()
            )
            setPreference(
                preferenceClearCenoCache,
                true,
                clickListener = getClickListenerForClearCenoCache()
            )
            setPreference(
                preferenceCenoNetworkDetails,
                true,
                clickListener = getClickListenerForCenoNetworkDetails()
            )
            setPreference(
                preferenceCenoEnableLog,
                true,
                changeListener = getChangeListenerForCenoSetting(OuinetKey.LOGFILE)
            )
            setPreference(
                preferenceCenoDownloadLog,
                true,
                clickListener = getClickListenerForCenoDownloadLog()
            )
            preferenceAboutOuinet?.summary = CenoSettings.getOuinetVersion(requireContext()) + " " +
                    CenoSettings.getOuinetBuildId(requireContext())
            preferenceAboutOuinetProtocol?.summary = "${CenoSettings.getOuinetProtocol(requireContext())}"
        }
    }


    private fun getClickListenerForMakeDefaultBrowser(): OnPreferenceClickListener {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
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
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, PrivacySettingsFragment())
                .addToBackStack(null)
                .commit()
            getActionBar().setTitle(R.string.tracker_category)
            true
        }
    }

    private fun getClickListenerForCustomization(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, CustomizationSettingsFragment())
                .addToBackStack(null)
                .commit()
            true
        }
    }

    private fun getClickListenerForSearch(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, InstalledSearchEnginesSettingsFragment())
                .addToBackStack(null)
                .commit()
            getActionBar().setTitle(R.string.preference_choose_search_engine)
            true
        }
    }

    private fun getClickListenerForDeleteBrowsingData(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, DeleteBrowsingDataFragment())
                .addToBackStack(null)
                .commit()
            getActionBar().setTitle(R.string.preferences_delete_browsing_data)
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
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, AboutFragment())
                .addToBackStack(null)
                .commit()
            getActionBar().setTitle(R.string.preferences_about_page)
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
                setTitle(context.getString(R.string.preferences_customize_amo_collection))
                setView(dialogView)
                setNegativeButton(R.string.customize_addon_collection_cancel) { dialog: DialogInterface, _ ->
                    dialog.cancel()
                }

                setPositiveButton(R.string.customize_addon_collection_ok) { _, _ ->
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
                        getString(R.string.toast_customize_addon_collection_done),
                        Toast.LENGTH_LONG,
                    ).show()

                    Handler().postDelayed(
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
                Toast.makeText(context, R.string.preferences_mobile_data_warning_disabled, LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(context, R.string.preferences_mobile_data_warning_enabled, LENGTH_SHORT).show()
            }
            true
        }
    }
     */

    private fun getClickListenerForDisableBatteryOpt(): OnPreferenceClickListener {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            OnPreferenceClickListener {
                PermissionHandler(requireContext()).requestBatteryOptimizationsOff(requireActivity())
                true
            }
        } else {
            defaultClickListener
        }
    }

    private fun getChangeListenerForCenoSetting( key : OuinetKey): OnPreferenceChangeListener {
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

    private fun getClickListenerForCenoGroupsCounts () : OnPreferenceClickListener {
        return OnPreferenceClickListener {
            val browserActivity = activity as BrowserActivity
            browserActivity.openToBrowser("${CenoSettings.SET_VALUE_ENDPOINT}/${OuinetKey.GROUPS_TXT.command}" , newTab = true)
            val entry: FragmentManager.BackStackEntry =
                browserActivity.supportFragmentManager.getBackStackEntryAt(0)
            browserActivity.supportFragmentManager.popBackStack(
                entry.id,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            browserActivity.supportFragmentManager.executePendingTransactions()
            true
        }
    }

    private fun getClickListenerForCenoNetworkDetails () : OnPreferenceClickListener {
        return OnPreferenceClickListener {
            val browserActivity = activity as BrowserActivity
            WebExtensionToolbarFeature.getBrowserAction(
                        requireContext(),
                        CenoWebExt.CENO_EXTENSION_ID
                    )?.invoke()
            (activity as BrowserActivity).popToFragmentIndex(0)
            true
        }
    }

    private fun getClickListenerForCenoDownloadLog(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            val download = DownloadState(url = "${CenoSettings.SET_VALUE_ENDPOINT}/${CenoSettings.LOGFILE_TXT}")
            if (sessionId != null) {
                requireContext().components.core.store.dispatch(ContentAction.UpdateDownloadAction(sessionId!!, download))
            }
            true
        }
    }

    companion object {
        /* CENO: Add a tag to keep track of whether this fragment is open */
        const val TAG = "SETTINGS"
        private const val AMO_COLLECTION_OVERRIDE_EXIT_DELAY = 3000L
        private const val SESSION_ID = "session_id"

        @JvmStatic
        protected fun Bundle.putSessionId(sessionId: String?) {
            putString(SESSION_ID, sessionId)
        }
        fun create(sessionId: String? = null) = SettingsFragment().apply {
            arguments = Bundle().apply {
                putSessionId(sessionId)
            }
        }
    }
}
