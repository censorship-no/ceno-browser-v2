/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings

import android.content.Context
import androidx.preference.PreferenceManager
import ie.equalit.ceno.R
import ie.equalit.ceno.settings.changeicon.appicons.AppIcon

object Settings {

    fun getOverrideAmoUser(context: Context): String =
        PreferenceManager.getDefaultSharedPreferences(context).getString(
            context.getString(R.string.pref_key_override_amo_user),
            ""
        ) ?: ""

    fun getOverrideAmoCollection(context: Context): String =
        PreferenceManager.getDefaultSharedPreferences(context).getString(
            context.getString(R.string.pref_key_override_amo_collection),
            ""
        ) ?: ""

    fun setOverrideAmoUser(context: Context, value: String) {
        val key = context.getString(R.string.pref_key_override_amo_user)
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(key, value)
            .apply()
    }

    fun setOverrideAmoCollection(context: Context, value: String) {
        val key = context.getString(R.string.pref_key_override_amo_collection)
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(key, value)
            .apply()
    }

    fun isAmoCollectionOverrideConfigured(context: Context): Boolean {
        return getOverrideAmoUser(context).isNotEmpty() && getOverrideAmoCollection(context).isNotEmpty()
    }

    fun setAppIcon(context: Context, value: String?) {
        val key = context.getString(R.string.pref_key_selected_app_icon)
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(key, value)
            .apply()
    }

    fun appIcon(context: Context) : AppIcon? {
        val componentName =  PreferenceManager.getDefaultSharedPreferences(context).getString(
            context.getString(R.string.pref_key_selected_app_icon), AppIcon.DEFAULT.componentName
        )
        return componentName?.let { AppIcon.from(it) }
    }

    fun getAppTheme(context: Context) : Int {
        val themeString = PreferenceManager.getDefaultSharedPreferences(context).getString(
                context.getString(R.string.pref_key_theme), context.getString(R.string.preferences_theme_default)
        )
        return themeString!!.toInt()
    }
}
