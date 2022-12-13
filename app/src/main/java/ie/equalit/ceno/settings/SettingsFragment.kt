/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import mozilla.components.support.ktx.android.view.showKeyboard
import ie.equalit.ceno.R
import ie.equalit.ceno.R.string.*
//import ie.equalit.ceno.R.string.pref_key_firefox_account
//import ie.equalit.ceno.R.string.pref_key_pair_sign_in
//import ie.equalit.ceno.R.string.pref_key_sign_in
import ie.equalit.ceno.autofill.AutofillPreference
import ie.equalit.ceno.components.ceno.PermissionHandler
import ie.equalit.ceno.ext.getPreferenceKey
import ie.equalit.ceno.ext.requireComponents
import kotlin.system.exitProcess

private typealias RBSettings = ie.equalit.ceno.settings.Settings

@Suppress("TooManyFunctions")
class SettingsFragment : PreferenceFragmentCompat() {

    interface ActionBarUpdater {
        fun updateTitle(titleResId: Int)
    }

    private val defaultClickListener = OnPreferenceClickListener { preference ->
        Toast.makeText(context, "${preference.title} Clicked", LENGTH_SHORT).show()
        true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()

        setupPreferences()
        getActionBar().apply{
            show()
            setTitle(R.string.settings)
        }
    }

    @Suppress("LongMethod") // Yep, this should be refactored.
    private fun setupPreferences() {
        /*
        val signInKey = requireContext().getPreferenceKey(pref_key_sign_in)
        val signInPairKey = requireContext().getPreferenceKey(pref_key_pair_sign_in)
        val firefoxAccountKey = requireContext().getPreferenceKey(pref_key_firefox_account)
         */
        val makeDefaultBrowserKey = requireContext().getPreferenceKey(pref_key_make_default_browser)
        val remoteDebuggingKey = requireContext().getPreferenceKey(pref_key_remote_debugging)
        val aboutPageKey = requireContext().getPreferenceKey(pref_key_about_page)
        val privacyKey = requireContext().getPreferenceKey(pref_key_privacy)
        val customAddonsKey = requireContext().getPreferenceKey(pref_key_override_amo_collection)
        val autofillPreferenceKey = requireContext().getPreferenceKey(R.string.pref_key_autofill)
        //val mobileDataKey = requireContext().getPreferenceKey(R.string.pref_key_mobile_data)
        val disableBatteryOptKey = requireContext().getPreferenceKey(R.string.pref_key_disable_battery_opt)
        val customizationKey = requireContext().getPreferenceKey(pref_key_customization)

        /*
        val preferenceSignIn = findPreference<Preference>(signInKey)
        val preferencePairSignIn = findPreference<Preference>(signInPairKey)
        val preferenceFirefoxAccount = findPreference<Preference>(firefoxAccountKey)
        */
        val preferenceMakeDefaultBrowser = findPreference<Preference>(makeDefaultBrowserKey)
        val preferenceRemoteDebugging = findPreference<SwitchPreferenceCompat>(remoteDebuggingKey)
        val preferenceAboutPage = findPreference<Preference>(aboutPageKey)
        val preferencePrivacy = findPreference<Preference>(privacyKey)
        val preferenceCustomAddons = findPreference<Preference>(customAddonsKey)
        val preferenceAutofill = findPreference<AutofillPreference>(autofillPreferenceKey)
        //val preferenceMobileData = findPreference<Preference>(mobileDataKey)
        val preferenceDisableBatteryOpt = findPreference<Preference>(disableBatteryOptKey)
        val preferenceCustomization = findPreference<Preference>(customizationKey)

        /*
        val accountManager = requireComponents.backgroundServices.accountManager
        if (accountManager.authenticatedAccount() != null) {
            preferenceSignIn?.isVisible = false
            preferencePairSignIn?.isVisible = false
            preferenceFirefoxAccount?.summary = accountManager.accountProfile()?.email.orEmpty()
            preferenceFirefoxAccount?.onPreferenceClickListener = getClickListenerForFirefoxAccount()
        } else {
            preferenceSignIn?.isVisible = true
            preferenceFirefoxAccount?.isVisible = false
            preferenceFirefoxAccount?.onPreferenceClickListener = null
            preferenceSignIn?.onPreferenceClickListener = getClickListenerForSignIn()
            preferencePairSignIn?.isVisible = true
            preferencePairSignIn?.onPreferenceClickListener = getClickListenerForPairingSignIn()
        }
        */

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
        //preferenceMobileData?.onPreferenceChangeListener = getChangeListenerForMobileData()
        preferenceCustomization?.onPreferenceClickListener = getClickListenerForCustomization()
        if (PermissionHandler(requireContext()).isIgnoringBatteryOptimizations()) {
                preferenceDisableBatteryOpt?.isVisible = false
        }
        else {
            preferenceDisableBatteryOpt?.onPreferenceClickListener = getClickListenerForDisableBatteryOpt()
        }
    }

    private fun getClickListenerForMakeDefaultBrowser(): OnPreferenceClickListener {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            OnPreferenceClickListener {
                val intent = Intent(
                    Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS
                )
                startActivity(intent)
                true
            }
        } else {
            defaultClickListener
        }
    }

    /*
    private fun getClickListenerForSignIn(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            requireComponents.services.accountsAuthFeature.beginAuthentication(requireContext())
            activity?.finish()
            true
        }
    }

    private fun getClickListenerForPairingSignIn(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, PairSettingsFragment())
                .addToBackStack(null)
                .commit()
            getActionBarUpdater().apply {
                updateTitle(R.string.pair_preferences)
            }
            true
        }
    }

    private fun getClickListenerForFirefoxAccount(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, AccountSettingsFragment())
                .addToBackStack(null)
                .commit()
            getActionBarUpdater().apply {
                updateTitle(R.string.account_settings)
            }
            true
        }
    }
     */

    private fun getClickListenerForPrivacy(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, PrivacySettingsFragment())
                .addToBackStack(null)
                .commit()
            getActionBar().setTitle(R.string.privacy_settings)
            true
        }
    }

    private fun getClickListenerForCustomization(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, CustomizationSettingsFragment())
                .addToBackStack(null)
                .commit()
            getActionBar().setTitle(R.string.customization_settings)
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
                .replace(android.R.id.content, AboutFragment())
                .addToBackStack(null)
                .commit()
            true
        }
    }

    /* TODO: ActionBarUpdater broken because no longer using SettingsActivity, find workaround */
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
                        Toast.LENGTH_LONG
                    ).show()

                    Handler().postDelayed(
                        {
                            exitProcess(0)
                        },
                        AMO_COLLECTION_OVERRIDE_EXIT_DELAY
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

    companion object {
        /* CENO: Add a tag to keep track of whether this fragment is open */
        const val TAG = "SETTINGS"
        private const val AMO_COLLECTION_OVERRIDE_EXIT_DELAY = 3000L
    }
}
