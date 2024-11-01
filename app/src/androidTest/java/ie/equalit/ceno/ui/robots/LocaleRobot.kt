package ie.equalit.ceno.ui.robots

import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import ie.equalit.ceno.R
import ie.equalit.ceno.ext.application
import ie.equalit.ceno.helpers.click
import junit.framework.TestCase.assertEquals


class LocaleRobot {

    fun verifyDefaultLocale() = assertDefaultLocale()
    fun verifyChangeLanguageDialog () = assertChangeLanguageDialog()
    fun verifyLocale(languageCode:String) = assertLocale(languageCode)

    class Transition {
        fun changeLanguage(language:String) {
            onView(withText(language)).click()
            onView(withText("Update")).click()
        }
    }
}
fun locale(interact: LocaleRobot.() -> Unit): LocaleRobot.Transition {
    LocaleRobot().interact()
    return LocaleRobot.Transition()
}
private fun assertDefaultLocale() {

    val defaultLocale = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.locales.get(0)
    assertEquals(defaultLocale?.language, "en")
}
private fun assertLocale(language:String) {
    val locale = AppCompatDelegate.getApplicationLocales().get(0)
    assertEquals(locale?.language, language)
}
private fun assertChangeLanguageDialog() {
    onView(withText(R.string.change_language)).check(matches(isDisplayed()))
}