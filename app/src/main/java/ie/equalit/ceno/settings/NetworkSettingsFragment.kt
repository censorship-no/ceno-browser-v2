/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings

import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.iterator
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import ie.equalit.ceno.BuildConfig
import ie.equalit.ceno.R
import ie.equalit.ceno.ext.getPreferenceKey
import mozilla.components.support.ktx.android.view.hideKeyboard
import mozilla.components.support.ktx.android.view.showKeyboard
import mozilla.components.support.ktx.kotlin.ifNullOrEmpty
import java.net.URLEncoder
import java.util.Locale
import java.util.regex.Pattern

class NetworkSettingsFragment : PreferenceFragmentCompat(), OuinetResponseListener {

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

        for (entry in BuildConfig.BT_BOOTSTRAP_EXTRAS) {
            btSourcesMap[Locale("", entry[0]).displayCountry] = entry[1]
        }

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

        val preferenceExtraBitTorrentBootstrap = findPreference<Preference>(extraBootstrapBittorrentKey)
        preferenceExtraBitTorrentBootstrap?.onPreferenceClickListener = getClickListenerForExtraBitTorrentBootstraps()
        preferenceExtraBitTorrentBootstrap?.summary = getBTPreferenceSummary()

        preferenceAboutOuinetProtocol?.summary = "${CenoSettings.getOuinetProtocol(requireContext())}"
        preferenceReachabilityStatus?.summary = CenoSettings.getReachabilityStatus(requireContext())
        preferenceLocalUdpEndpoint?.summary = CenoSettings.getLocalUdpEndpoint(requireContext()).ifNullOrEmpty { getString(R.string.not_applicable) }
        preferenceExternalUdpEndpoint?.summary = CenoSettings.getExternalUdpEndpoint(requireContext()).ifNullOrEmpty { getString(R.string.not_applicable) }
        preferencePublicUdpEndpoint?.summary = CenoSettings.getPublicUdpEndpoint(requireContext()).ifNullOrEmpty { getString(R.string.not_applicable) }
        preferenceUpnpStatus?.summary = CenoSettings.getUpnpStatus(requireContext())

    }

    private fun getClickListenerForExtraBitTorrentBootstraps(): Preference.OnPreferenceClickListener {
        return Preference.OnPreferenceClickListener {

            val customDialogView = View.inflate(context, R.layout.custom_extra_bt_dialog, null)
            val customBTSourcesView = customDialogView.findViewById<EditText>(R.id.bootstrap)

            val extraBtOptionsDialogView = View.inflate(context, R.layout.extra_bt_options_dialog, null)
            val linearLayout = extraBtOptionsDialogView.findViewById<LinearLayout>(R.id.linear_layout)
            val tvCustomSource = extraBtOptionsDialogView.findViewById<TextView>(R.id.tv_custom_sources)

            val alertDialog1 = AlertDialog.Builder(requireContext()).apply {
                setTitle(getString(R.string.select_extra_bt_source))
                setView(extraBtOptionsDialogView)
                setNegativeButton(R.string.customize_addon_collection_cancel) { dialog: DialogInterface, _ -> dialog.cancel() }
                setPositiveButton(R.string.customize_add_bootstrap_save) { _, _ ->
                    val allSelectedIPs = mutableListOf<String>()
                    for (child in linearLayout.iterator()) {
                        if (child is CheckBox && child.isChecked) {
                            allSelectedIPs.add(
                                btSourcesMap.entries.find { e ->
                                    e.key.lowercase() == child.text.toString().trim().lowercase()
                                }?.value ?: child.text.toString().trim())
                        }
                    }

                    CenoSettings.ouinetClientRequest(
                        context,
                        OuinetKey.EXTRA_BOOTSTRAPS,
                        OuinetValue.OTHER,
                        URLEncoder.encode(allSelectedIPs.joinToString(" "), "UTF-8"),
                        this@NetworkSettingsFragment
                    )
                }

                btSourcesMap.forEach {
                    linearLayout.addView(
                        CheckBox(activity).apply {
                            text = Locale("", it.key).displayCountry
                            isChecked = CenoSettings.getLocalBTSources(requireContext())?.contains(it.value) == true
                            isAllCaps = false
                        }
                    )
                }

                // add custom sources
                CenoSettings.getLocalBTSources(requireContext())?.forEach {
                    it.let { source ->
                        if (source.trim().isNotEmpty() && !btSourcesMap.containsValue(source.trim())) {
                            linearLayout.addView(
                                CheckBox(activity).apply {
                                    text = it
                                    isChecked = true
                                    isAllCaps = false
                                }
                            )
                        }
                    }
                }

                tvCustomSource.setOnClickListener {

                    (customDialogView.parent as? ViewGroup)?.removeView(customDialogView)

                    val alertDialog2 = AlertDialog.Builder(context).apply {
                        setTitle(context.getString(R.string.customize_extra_bittorrent_bootstrap))
                        setView(customDialogView)
                        setNegativeButton(R.string.customize_addon_collection_cancel) { dialog: DialogInterface, _ ->
                            customBTSourcesView.hideKeyboard()
                            dialog.cancel()
                        }
                        setPositiveButton(R.string.customize_add_bootstrap_save) { _, _ ->
                            val ipAddresses = customBTSourcesView.text.toString().trim().split(",")

                            for (ipAddress in ipAddresses) {
                                // Pattern for validating IPs
                                val ipPattern = Pattern.compile(
                                    """^(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)\.(25[0-5]|2[0-4]\d|[01]?\d\d?)$"""
                                )
                                if (!ipPattern.matcher(ipAddress.trim()).matches()) {
                                    Toast.makeText(requireContext(), getString(R.string.bt_invalid_ip_error), Toast.LENGTH_SHORT).show()
                                    customBTSourcesView.hideKeyboard()
                                    return@setPositiveButton
                                }
                            }

                            // Add IPs to the list of IP sources
                            for (ip in ipAddresses) {
                                linearLayout.addView(
                                    CheckBox(activity).apply {
                                        text = ip
                                        isChecked = true
                                        isAllCaps = false
                                    }
                                )
                            }

                            customBTSourcesView.hideKeyboard()
                        }
                        customBTSourcesView.requestFocus()
                        customBTSourcesView.showKeyboard()
                        create()
                    }

                    alertDialog2.show()
                }
                create()
            }

            alertDialog1.show()

            true
        }
    }

    private fun getPreference(key: Int): Preference? {
        val prefKey = requireContext().getPreferenceKey(key)
        return findPreference(prefKey)
    }

    override fun onBTChangeSuccess(source: String) {
        CenoSettings.saveExtraBTBootstrapToLocal(requireContext(), source.split("+").toTypedArray())
        getPreference(R.string.pref_key_ouinet_extra_bittorrent_bootstraps)?.summary = getBTPreferenceSummary()
        Toast.makeText(requireContext(), getString(R.string.ouinet_client_fetch_success), Toast.LENGTH_SHORT).show()
    }

    override fun onErrorResponse() {
        Toast.makeText(context, resources.getString(R.string.ouinet_client_fetch_fail), Toast.LENGTH_SHORT).show()
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
}
