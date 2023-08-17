/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings

import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import ie.equalit.ceno.R
import ie.equalit.ceno.ext.getPreferenceKey
import mozilla.components.support.ktx.android.view.showKeyboard

class NetworkSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.network_detail_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        setupPreferences()
        getActionBar().apply{
            show()
            setTitle(R.string.preferences_ceno_network_config)
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(requireContext(), R.color.ceno_action_bar)))
        }
    }

    private fun setupPreferences() {

        val preferenceAboutOuinetProtocol = getPreference(R.string.pref_key_about_ouinet_protocol)
        val preferenceReachabilityStatus = getPreference(R.string.pref_key_ouinet_reachability_status)
        val preferenceLocalUdpEndpoint = getPreference(R.string.pref_key_ouinet_local_udp_endpoints)
        val preferenceExternalUdpEndpoint = getPreference(R.string.pref_key_ouinet_external_udp_endpoints)
        val preferencePublicUdpEndpoint = getPreference(R.string.pref_key_ouinet_public_udp_endpoints)
        val preferenceUpnpStatus = getPreference(R.string.pref_key_ouinet_upnp_status)
        val extraBootstrapBittorrentKey = requireContext().getPreferenceKey(R.string.pref_key_ouinet_extra_bittorrent_bootstraps)

        val preferenceExtraBitTorrrentBootstrap = findPreference<Preference>(extraBootstrapBittorrentKey)
        preferenceExtraBitTorrrentBootstrap?.onPreferenceClickListener = getClickListenerForExtraBitTorrentBootstraps()

        preferenceAboutOuinetProtocol?.summary = "${CenoSettings.getOuinetProtocol(requireContext())}"
        preferenceReachabilityStatus?.summary = CenoSettings.getReachabilityStatus(requireContext())
        preferenceLocalUdpEndpoint?.summary = CenoSettings.getLocalUdpEndpoint(requireContext())
        preferenceExternalUdpEndpoint?.summary = CenoSettings.getExternalUdpEndpoint(requireContext())
        preferencePublicUdpEndpoint?.summary = CenoSettings.getPublicUdpEndpoint(requireContext())
        preferenceUpnpStatus?.summary = CenoSettings.getUpnpStatus(requireContext())

        preferenceLocalUdpEndpoint?.isVisible = CenoSettings.getLocalUdpEndpoint(requireContext()) != null
        preferenceExternalUdpEndpoint?.isVisible = CenoSettings.getExternalUdpEndpoint(requireContext()) != null
        preferencePublicUdpEndpoint?.isVisible = CenoSettings.getPublicUdpEndpoint(requireContext()) != null

    }

    private fun getClickListenerForExtraBitTorrentBootstraps(): Preference.OnPreferenceClickListener {
        return Preference.OnPreferenceClickListener {
            val context = requireContext()
            val dialogView = View.inflate(context, R.layout.extra_bittorrent_bootstrap_override_dialog, null)
            val bootstrapView = dialogView.findViewById<EditText>(R.id.bootstrap)

            AlertDialog.Builder(context).apply {
                setTitle(context.getString(R.string.customize_extra_bittorrent_bootstrap))
                setView(dialogView)
                setNegativeButton(R.string.customize_addon_collection_cancel) { dialog: DialogInterface, _ ->
                    dialog.cancel()
                }

                setPositiveButton(R.string.customize_add_bootstrap_save) { _, _ ->

                    CenoSettings.ouinetClientRequest(
                        requireContext(),
                        OuinetKey.EXTRA_BOOTSTRAPS,
                        OuinetValue.OTHER,
                        bootstrapView.text.toString().trim()
                    )

                    CenoSettings.setExtraBitTorrentBootstrap(
                        context,
                        arrayOf(bootstrapView.text.toString())
                    )

                    getPreference(R.string.pref_key_ouinet_extra_bittorrent_bootstraps)?.summary = CenoSettings.getExtraBitTorrentBootstrap(requireContext())
                }

                bootstrapView.setText(
                    CenoSettings.getExtraBitTorrentBootstrap(
                        context
                    )?.trim()
                )
                bootstrapView.requestFocus()
                bootstrapView.showKeyboard()
                create()
            }.show()
            true
        }
    }

    private fun getPreference(key : Int) : Preference? {
        val prefKey = requireContext().getPreferenceKey(key)
        return findPreference(prefKey)
    }

    private fun getActionBar() = (activity as AppCompatActivity).supportActionBar!!
}
