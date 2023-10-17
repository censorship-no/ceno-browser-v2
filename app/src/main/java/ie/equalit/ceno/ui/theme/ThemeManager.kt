package ie.equalit.ceno.ui.theme

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION.SDK_INT
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

    abstract fun applyStatusBarTheme()

    fun updateNavigationBar(window: Window, context: Context) {
        window.navigationBarColor = context.getColorFromAttr(R.attr.layer1)
    }

    fun clearLightSystemBars(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getWindowInsetsController().isAppearanceLightStatusBars = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API level can display handle light navigation bar color
            window.getWindowInsetsController().isAppearanceLightNavigationBars = false
        }
    }

    abstract fun applyTheme(toolbar: BrowserToolbar, context: Context)

    fun updateLightSystemBars(window: Window, context: Context) {
        if (SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = context.getColorFromAttr(R.attr.layer1)
            window.getWindowInsetsController().isAppearanceLightStatusBars = true
        } else {
            window.statusBarColor = Color.BLACK
        }

        if (SDK_INT >= Build.VERSION_CODES.O) {
            // API level can display handle light navigation bar color
            window.getWindowInsetsController().isAppearanceLightNavigationBars = true
            updateNavigationBar(window, context)
        }
    }

    fun updateDarkSystemBars(window: Window, context: Context) {
        if (SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = context.getColorFromAttr(R.attr.layer1)
            window.getWindowInsetsController().isAppearanceLightStatusBars = false
        } else {
            window.statusBarColor = Color.BLACK
        }

        if (SDK_INT >= Build.VERSION_CODES.O) {
            // API level can display handle light navigation bar color
            window.getWindowInsetsController().isAppearanceLightNavigationBars = true
            updateNavigationBar(window, context)
        }
    }

    abstract fun applyStatusBarThemeTabsTray()
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

            applyStatusBarTheme()
        }

    override fun applyStatusBarThemeTabsTray() {
        when (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO,
            -> {
                updateLightSystemBars(activity.window, activity)
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED, //we assume dark mode is default
            Configuration.UI_MODE_NIGHT_YES -> {
                updateDarkSystemBars(activity.window, activity)
            }
        }
    }

    override fun applyStatusBarTheme() {
        when (currentMode) {
            BrowsingMode.Normal -> {
                when (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                    Configuration.UI_MODE_NIGHT_NO,
                    -> {
                        updateLightSystemBars(activity.window, currentContext)
                    }
                    Configuration.UI_MODE_NIGHT_UNDEFINED, //we assume dark mode is default
                    Configuration.UI_MODE_NIGHT_YES -> {
                        updateDarkSystemBars(activity.window, currentContext)
                    }
                }
            }
            BrowsingMode.Personal -> {
                updateDarkSystemBars(activity.window, currentContext)
            }
        }
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