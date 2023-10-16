package ie.equalit.ceno.ui.theme

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Window
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import ie.equalit.ceno.BrowserActivity
import ie.equalit.ceno.ExternalAppBrowserActivity
import ie.equalit.ceno.R
import ie.equalit.ceno.browser.BrowsingMode
import ie.equalit.ceno.ext.cenoPreferences
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.content.res.resolveAttribute
import mozilla.components.support.ktx.android.view.getWindowInsetsController
import java.util.logging.Logger

abstract class ThemeManager {
    abstract var currentMode: BrowsingMode


    /**
     * Handles status bar theme change since the window does not dynamically recreate
     */
    fun applyStatusBarTheme(activity: Activity) = applyStatusBarTheme(activity.window, activity)

    fun applyStatusBarTheme(window: Window, context: Context) {
        when (currentMode) {
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

    abstract fun applyTheme(toolbar: BrowserToolbar, context: Context)

}

class DefaultThemeManager(
    mode: BrowsingMode,
    private val activity: Activity
) : ThemeManager() {

    private var currentContext:Context = activity

    private val personalThemeContext = ContextThemeWrapper(activity.applicationContext, R.style.PersonalTheme)

    override var currentMode: BrowsingMode = mode
        set(value) {
            //apply theme to fragment rather than recreating activity
            field = value
            if (value == BrowsingMode.Personal)
                currentContext = personalThemeContext
            else
                currentContext = activity
        }

    override fun applyTheme(toolbar: BrowserToolbar, context: Context) {
        toolbar.background = ContextCompat.getDrawable(currentContext, R.drawable.toolbar_dark_background)
        toolbar.display.setUrlBackground(ContextCompat.getDrawable(currentContext, R.drawable.url_background))

        var color = TypedValue()
        currentContext.theme.resolveAttribute(R.attr.textPrimary, color, true)
        Log.d("THEME", "$color.resourceId")
        var textPrimary = ContextCompat.getColor(currentContext, currentContext.theme.resolveAttribute(R.attr.textPrimary))
        var textSecondary = ContextCompat.getColor(currentContext, currentContext.theme.resolveAttribute(R.attr.textSecondary))

        toolbar.edit.colors = toolbar.edit.colors.copy(
            text = textPrimary,
            hint = textSecondary
        )
        toolbar.display.colors = toolbar.display.colors.copy(
            text = textPrimary,
            hint = textSecondary,
            securityIconSecure = textPrimary,
            securityIconInsecure = textPrimary,
            menu = textPrimary
        )
    }
}