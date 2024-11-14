package ie.equalit.ceno.ui.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import ie.equalit.ceno.R
import ie.equalit.ceno.helpers.TestAssetHelper
import ie.equalit.ceno.helpers.assertIsChecked
import ie.equalit.ceno.helpers.click
import ie.equalit.ceno.helpers.hasCousin
import org.hamcrest.CoreMatchers.allOf

/**
 * Implementation of Robot Pattern for the settings privacy menu.
 */
class SettingsViewDeveloperToolsRobot {

    fun verifyDeveloperToolsUpButton() = assertDeveloperToolsUpButton()
    fun verifyDeveloperToolsHeading() = assertDeveloperToolsHeading()
    fun verifyRemoteDebugging() = assertRemoteDebugging()
    fun verifyExportOuinetLog() = assertExportOuinetLog()

    // toggleRemoteDebugging does not yet verify that the debug service is started
    // server runs on port 6000
    fun toggleRemoteDebuggingOn() : ViewInteraction {
        remoteDebuggingToggle().assertIsChecked(false)
        remoteDebuggingToggle().click()
        return remoteDebuggingToggle().assertIsChecked(true)
    }

    fun toggleRemoteDebuggingOff() : ViewInteraction {
        remoteDebuggingToggle().assertIsChecked(true)
        remoteDebuggingToggle().click()
        return remoteDebuggingToggle().assertIsChecked(false)
    }

    class Transition {
        fun settingsViewDeveloperTools(interact: SettingsViewDeveloperToolsRobot.() -> Unit): Transition {
            return Transition()
        }
    }
}

private fun developerToolsHeading() = Espresso.onView(withText(R.string.developer_tools_category))
private fun remoteDebuggingToggle() = Espresso.onView(
    allOf(
        withId(R.id.switchWidget), hasCousin(
            withText(R.string.preferences_remote_debugging)
        )
    )
)
private fun exportOuinetLog() = Espresso.onView(withText(R.string.preferences_ceno_download_log))
private fun assertDeveloperToolsUpButton() {
    mDevice.wait(Until.findObject(By.text("Navigate up")), TestAssetHelper.waitingTimeShort)
}
private fun assertDeveloperToolsHeading() = developerToolsHeading()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertRemoteDebugging() = remoteDebuggingToggle()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertExportOuinetLog() = exportOuinetLog()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
