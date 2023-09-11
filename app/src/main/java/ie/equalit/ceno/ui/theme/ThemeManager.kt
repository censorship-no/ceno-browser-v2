package ie.equalit.ceno.ui.theme

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Window
import androidx.annotation.StyleRes
import ie.equalit.ceno.BrowserActivity
import ie.equalit.ceno.ExternalAppBrowserActivity
import ie.equalit.ceno.R
import ie.equalit.ceno.browser.BrowsingMode
import ie.equalit.ceno.ext.cenoPreferences
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.view.getWindowInsetsController

abstract class ThemeManager {
    abstract val themedContext: Context
    abstract var currentTheme: BrowsingMode

    /**
     * Returns the style resource corresponding to the [currentTheme].
     */
    @get:StyleRes
    val currentThemeResource get() = when (currentTheme) {
        BrowsingMode.Normal -> R.style.NormalTheme
        BrowsingMode.Personal -> R.style.PersonalTheme
    }

    /**
     * Handles status bar theme change since the window does not dynamically recreate
     */
    fun applyStatusBarTheme(activity: Activity) = applyStatusBarTheme(activity.window, activity)

    fun applyStatusBarTheme(window: Window, context: Context) {
        when (currentTheme) {
            BrowsingMode.Normal -> {
                clearLightSystemBars(window)
                updateNavigationBar(window, context = context)
            }
            BrowsingMode.Personal -> {
                Log.d("MODE", context.resources.configuration.uiMode.toString())
            }
        }
    }


    private fun clearLightSystemBars(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getWindowInsetsController().isAppearanceLightStatusBars = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API level can display handle light navigation bar color
            window.getWindowInsetsController().isAppearanceLightNavigationBars = false
        }
    }

    private fun updateNavigationBar(window: Window, context: Context) {
        window.navigationBarColor = context.getColorFromAttr(R.attr.layer1)
    }

    fun setActivityTheme(activity: Activity) {
        activity.setTheme(currentThemeResource)
    }

    abstract fun getContext() : Context

}

class DefaultThemeManager(
    mode: BrowsingMode,
    private val activity: Activity
) : ThemeManager() {

    override val themedContext:Context = ContextThemeWrapper(activity, R.style.PersonalTheme)
    override var currentTheme: BrowsingMode = mode
        set(value) {
            //apply theme to fragment rather than recreating activity
            field = value

        }

    override fun getContext() : Context {
        return when(currentTheme) {
            BrowsingMode.Normal -> activity
            BrowsingMode.Personal -> themedContext
        }
    }

}