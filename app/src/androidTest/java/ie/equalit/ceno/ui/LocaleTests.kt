package ie.equalit.ceno.ui

import ie.equalit.ceno.helpers.BrowserActivityTestRule
import ie.equalit.ceno.helpers.RetryTestRule
import ie.equalit.ceno.helpers.click
import ie.equalit.ceno.ui.robots.locale
import ie.equalit.ceno.ui.robots.navigationToolbar
import ie.equalit.ceno.ui.robots.onboarding
import ie.equalit.ceno.ui.robots.selectLanguageButton
import ie.equalit.ceno.ui.robots.standby
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LocaleTests {

    @get:Rule
    val activityTestRule = BrowserActivityTestRule()

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(1)

    @Before
    fun setUp() {
        standby {}.waitForStandbyIfNeeded()
    }

    @Test
    fun changeLocaleFromStartTooltip() {
        onboarding {
            verifyStartTooltipExists()
            verifyStartTooltip()
            //verify locale
            locale {
                verifyDefaultLocale()
            }
            //change locale
            selectLanguageButton().click()
            locale {
                verifyChangeLanguageDialog()
            }.changeLanguage("Persian")
            standby {}.waitForStandbyIfNeeded()
            //verify new locale
            locale {
                verifyLocale("fa")
            }
        }
    }

    @Test
    fun changeLocaleFromSettings() {
        onboarding {
        }.skipOnboardingIfNeeded()
        locale {
            verifyDefaultLocale()
        }
        navigationToolbar {
        }.openThreeDotMenu {
        }.openSettings {
            Thread.sleep(5000)
            clickDownRecyclerView(9)
            verifyChangeLanguageButton()
            clickChangeLanguageButton()
        }
        locale {
            verifyChangeLanguageDialog()
        }.changeLanguage("Persian")
        standby {}.waitForStandbyIfNeeded()
        //verify new locale
        locale {
            verifyLocale("fa")
        }
    }
}