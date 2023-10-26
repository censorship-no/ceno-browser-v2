/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.ext

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import ie.equalit.ceno.autofill.AutofillPreference

/**
 * Get the Preference object corresponding to a key
 */
fun PreferenceFragmentCompat.getPreference(key: Int): Preference? {
    val prefKey = requireContext().getPreferenceKey(key)
    return findPreference(prefKey)
}

/**
 * Get the SwitchPreferenceCompat object corresponding to a key
 */
fun PreferenceFragmentCompat.getSwitchPreferenceCompat(key: Int): SwitchPreferenceCompat? {
    val prefKey = requireContext().getPreferenceKey(key)
    return findPreference(prefKey)
}

/**
 * Get the AutofillPreference object corresponding to a key
 */
fun PreferenceFragmentCompat.getAutofillPreference(key: Int): AutofillPreference? {
    val prefKey = requireContext().getPreferenceKey(key)
    return findPreference(prefKey)
}