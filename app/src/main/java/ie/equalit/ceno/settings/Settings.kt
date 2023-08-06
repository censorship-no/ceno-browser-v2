/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings

import android.content.Context
import androidx.preference.PreferenceManager
import ie.equalit.ceno.R

object Settings {

    fun getAppTheme(context: Context) : Int {
        val themeString = PreferenceManager.getDefaultSharedPreferences(context).getString(
                context.getString(R.string.pref_key_theme), context.getString(R.string.preferences_theme_default)
        )
        return themeString!!.toInt()
    }
}
