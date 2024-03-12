/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import ie.equalit.ceno.BuildConfig
import ie.equalit.ceno.R
import ie.equalit.ceno.ext.getPreferenceKey
import ie.equalit.ceno.ext.requireComponents
import ie.equalit.ouinet.Config
import ie.equalit.ouinet.Ouinet.RunningState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.ktx.kotlin.ifNullOrEmpty
import java.util.Locale

class NetworkSettingsFragment : PreferenceFragmentCompat() {

    private var hasOuinetStopped: Boolean = false
    private var wasLogEnabled: Boolean = false
    private var bridgeModeChanged: Boolean = false
    private lateinit var bridgeAnnouncementDialog: AlertDialog
    private var logFileReset:Boolean = false
    private var logLevelReset:Boolean = false

    // This variable stores a map of all the sources from local.properties
    private val btSourcesMap = mutableMapOf<String, String>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.network_detail_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()

        getActionBar().apply {
            show()
            setTitle(R.string.preferences_ceno_network_config)
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(requireContext(), R.color.ceno_action_bar)))
        }

        for (entry in BuildConfig.BT_BOOTSTRAP_EXTRAS) btSourcesMap[Locale("", entry[0]).displayCountry] = entry[1]
        setupPreferences()
    }

    private fun setupPreferences() {

        val preferenceAboutOuinetProtocol = getPreference(R.string.pref_key_about_ouinet_protocol)
        val preferenceReachabilityStatus = getPreference(R.string.pref_key_ouinet_reachability_status)
        val preferenceLocalUdpEndpoint = getPreference(R.string.pref_key_ouinet_local_udp_endpoints)
        val preferenceExternalUdpEndpoint = getPreference(R.string.pref_key_ouinet_external_udp_endpoints)
        val preferencePublicUdpEndpoint = getPreference(R.string.pref_key_ouinet_public_udp_endpoints)
        val preferenceUpnpStatus = getPreference(R.string.pref_key_ouinet_upnp_status)
        val extraBootstrapBittorrentKey = requireContext().getPreferenceKey(R.string.pref_key_ouinet_extra_bittorrent_bootstraps)
        val preferenceBridgeAnnouncement = getPreference(R.string.pref_key_bridge_announcement)
        preferenceBridgeAnnouncement?.onPreferenceChangeListener = getChangeListenerForBridgeAnnouncement()
        preferenceBridgeAnnouncement?.summary = getString(R.string.bridge_mode_ip_warning_text)

        val preferenceExtraBitTorrentBootstrap = findPreference<Preference>(extraBootstrapBittorrentKey)
        preferenceExtraBitTorrentBootstrap?.onPreferenceClickListener = getClickListenerForExtraBitTorrentBootstraps()

        preferenceAboutOuinetProtocol?.summary = "${CenoSettings.getOuinetProtocol(requireContext())}"
        preferenceReachabilityStatus?.summary = CenoSettings.getReachabilityStatus(requireContext())
        preferenceLocalUdpEndpoint?.summary = CenoSettings.getLocalUdpEndpoint(requireContext()).ifNullOrEmpty { getString(R.string.not_applicable) }
        preferenceExternalUdpEndpoint?.summary = CenoSettings.getExternalUdpEndpoint(requireContext()).ifNullOrEmpty { getString(R.string.not_applicable) }
        preferencePublicUdpEndpoint?.summary = CenoSettings.getPublicUdpEndpoint(requireContext()).ifNullOrEmpty { getString(R.string.not_applicable) }
        preferenceUpnpStatus?.summary = CenoSettings.getUpnpStatus(requireContext())
        preferenceExtraBitTorrentBootstrap?.summary = getBTPreferenceSummary()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bridgeAnnouncementDialog = UpdateBridgeAnnouncementDialog(requireContext()).getDialog()
        if (arguments?.getBoolean(scrollToBridge) == true) {
            getPreference(R.string.pref_key_bridge_announcement)?.let {
                scrollToPreference(it)
            }
        }
        view.consumeFrom(requireComponents.appStore, viewLifecycleOwner) {
            if (it.ouinetStatus == RunningState.Started) {
                bridgeAnnouncementDialog.dismiss()
            }
        }
    }

    private fun setLogFileAndLevel (newValue : Boolean) {
        // network request to update preference value
        CenoSettings.ouinetClientRequest(
            context = requireContext(),
            key = OuinetKey.LOGFILE,
            newValue = if(newValue) OuinetValue.ENABLED else OuinetValue.DISABLED,
            null,
            object : OuinetResponseListener {
                override fun onSuccess(message: String, data: Any?) {
                    logFileReset = !newValue
                }
                override fun onError() {
                    /* Still flag reset complete on error, since not flagging will cause dialog to hang */
                    logFileReset = !newValue
                }
            }
        )
        // network request to update log level based on preference value
        CenoSettings.ouinetClientRequest(
            context = requireContext(),
            key = OuinetKey.LOG_LEVEL,
            newValue = null,
            stringValue = if(newValue) Config.LogLevel.DEBUG.toString() else Config.LogLevel.INFO.toString(),
            object : OuinetResponseListener {
                override fun onSuccess(message: String, data: Any?) {
                    logLevelReset = !newValue
                }
                override fun onError() {
                    /* Still flag reset complete on error, since not flagging will cause dialog to hang */
                    logLevelReset = !newValue
                }
            }
        )
    }

    private fun monitorOuinet() {
        lifecycleScope.launch {
            while (!hasOuinetStopped) {
                delay(DELAY_ONE_SECOND)
            }
            requireComponents.ouinet.setConfig()
            requireComponents.ouinet.setBackground(requireContext())
            requireComponents.ouinet.background.startup {
                hasOuinetStopped = false
                /* if debug log previously enabled, re-enable it after startup completes */
                if (wasLogEnabled) {
                    CenoSettings.setCenoEnableLog(requireContext(), true)
                    setLogFileAndLevel(true)
                }
            }
        }
    }

    private fun getChangeListenerForBridgeAnnouncement(): Preference.OnPreferenceChangeListener {
        return Preference.OnPreferenceChangeListener { _, _ ->
            /* Resetting the log settings is a workaround for ouinet logs disappearing after toggling bridge mode,
            * https://gitlab.com/censorship-no/ceno-browser/-/merge_requests/127#note_1795759444
            * TODO: identify root cause of this behavior and remove workaround
            * */
            wasLogEnabled = CenoSettings.isCenoLogEnabled(requireContext())
            if (wasLogEnabled) {
                CenoSettings.setCenoEnableLog(requireContext(), false)
                setLogFileAndLevel(false)
            }
            monitorOuinet()
            lifecycleScope.launch {
                if (wasLogEnabled) {
                    while (!(logFileReset && logLevelReset)) {
                        delay(DELAY_ONE_SECOND)
                        println("logFileReset && logLevelReset = $logFileReset && $logLevelReset")
                    }
                }
                requireComponents.ouinet.background.shutdown(false) {
                    hasOuinetStopped = true
                }
            }
            bridgeModeChanged = true
            bridgeAnnouncementDialog.show()
            true
        }
    }

    private fun getClickListenerForExtraBitTorrentBootstraps(): Preference.OnPreferenceClickListener {
        return Preference.OnPreferenceClickListener {
            val extraBTBootstrapsDialog = ExtraBTBootstrapsDialog(requireContext(), btSourcesMap) {
                getPreference(R.string.pref_key_ouinet_extra_bittorrent_bootstraps)?.summary =
                    getBTPreferenceSummary()
            }
            extraBTBootstrapsDialog.getDialog().show()

            true
        }
    }

    private fun getPreference(key: Int): Preference? {
        val prefKey = requireContext().getPreferenceKey(key)
        return findPreference(prefKey)
    }

    private fun getBTPreferenceSummary(): String {
        var summary = ""

        CenoSettings.getLocalBTSources(requireContext())?.forEach {
            summary = if (btSourcesMap.values.contains(it)) {
                "$summary ${btSourcesMap.entries.find { e -> e.value.trim() == it }?.key?.replace(" ", "")}"
            } else {
                "$summary $it"
            }
        }

        return when {
            summary.trim().isEmpty() -> getString(R.string.bt_sources_none)
            else -> summary.trim().replace(" ", ", ")
        }
    }

    private fun getActionBar() = (activity as AppCompatActivity).supportActionBar!!

    companion object {
        const val scrollToBridge = "scrollToBridge"
        const val DELAY_ONE_SECOND = 1000L
    }
}
