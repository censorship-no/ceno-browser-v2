/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import ie.equalit.ceno.R
import ie.equalit.ceno.ext.getPreferenceKey
import ie.equalit.ceno.settings.changeicon.ChangeIconFragment

class CustomizationSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.customization_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        setupPreferences()
        getActionBar().setTitle(R.string.customization_settings)
    }

    private fun setupPreferences() {
        val changeAppIconKey = requireContext().getPreferenceKey(R.string.pref_key_change_app_icon)
        val preferenceChangeAppIcon = findPreference<Preference>(changeAppIconKey)
        preferenceChangeAppIcon?.onPreferenceClickListener = getClickListenerForChangeAppIcon()
    }

    private fun getClickListenerForChangeAppIcon(): Preference.OnPreferenceClickListener {
        return Preference.OnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, ChangeIconFragment())
                .addToBackStack(null)
                .commit()
            getActionBar().setTitle(R.string.preferences_change_app_icon)
            true
        }
    }

    private fun getActionBar() = (activity as AppCompatActivity).supportActionBar!!
}
