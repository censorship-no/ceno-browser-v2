/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings

import android.content.Context
import androidx.preference.PreferenceManager

object CustomPreferenceManager {

    fun getBoolean(context: Context, key: Int, defaultValue: Boolean = false): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            context.getString(key), defaultValue
        )

    fun setBoolean(context: Context, key: Int, value: Boolean) =
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(context.getString(key), value)
            .apply()

}