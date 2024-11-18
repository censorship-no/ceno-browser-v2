package ie.equalit.ceno.ui.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
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
    fun verifyExportOuinetLogDescription() = assertExportOuinetLogDescription()
    fun verifyExportOuinetLogDownload() = assertExportOuinetLogDownload()
    fun verifyExportOuinetLogView() = assertExportOuinetLogView()
    fun verifyAnnouncementSource() = assertAnnouncementSource()
    fun verifyAnnouncementSourceSummary() = assertAnnouncementSourceSummary()
    fun verifyAnnouncementExpiration() = assertAnnouncementExpiration()
    fun verifyAnnouncementOption1() = assertAnnouncementOption1()
    fun verifyAnnouncementOption2() = assertAnnouncementOption2()
    fun verifyAnnouncementOption3() = assertAnnouncementOption3()

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

    fun toggleAnnouncementExpirationOn() : ViewInteraction {
        announcementExpiration().assertIsChecked(false)
        announcementExpiration().click()
        return announcementExpiration().assertIsChecked(true)
    }

    fun toggleAnnouncementExpirationOff() : ViewInteraction {
        announcementExpiration().assertIsChecked(true)
        announcementExpiration().click()
        return announcementExpiration().assertIsChecked(false)
    }

    fun clickExportOuinetLog() {
        exportOuinetLog().click()
    }

    fun clickAnnouncementSource() {
        announcementSource().click()
    }

    fun clickCancelDialog() {
        cancelDialogButton().click()
    }

    class Transition {
        fun settingsViewDeveloperTools(interact: SettingsViewDeveloperToolsRobot.() -> Unit): Transition {
            return Transition()
        }

        fun goBack(interact: SettingsViewRobot.() -> Unit): SettingsViewRobot.Transition {
            mDevice.pressBack()
            SettingsViewRobot().interact()
            return SettingsViewRobot.Transition()
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
private fun exportOuinetLogDescription() = Espresso.onView(withText(R.string.ouinet_log_file_prompt_desc))
private fun exportOuinetLogDownload() = Espresso.onView(withText(R.string.download_logs))
private fun exportOuinetLogView() = Espresso.onView(withText(R.string.view_logs))
private fun announcementSource() = Espresso.onView(withText(R.string.preferences_announcement_source))
private fun announcementSourceSummary() = Espresso.onView(withText(R.string.preferences_announcement_source_summary))
private fun announcementExpiration() = Espresso.onView(
    allOf(
        withId(R.id.switchWidget), hasCousin(
            withText(R.string.preferences_announcement_expire_disable)
        )
    )
)
private fun announcementOption1() = Espresso.onView(withText(R.string.preferences_announcement_source_option_1))
private fun announcementOption2() = Espresso.onView(withText(R.string.preferences_announcement_source_option_2))
private fun announcementOption3() = Espresso.onView(withText(R.string.preferences_announcement_source_option_3))
private fun cancelDialogButton() = mDevice.findObject(
    UiSelector().resourceId("android:id/button2"),
)
private fun assertDeveloperToolsUpButton() {
    mDevice.wait(Until.findObject(By.text("Navigate up")), TestAssetHelper.waitingTimeShort)
}
private fun assertDeveloperToolsHeading() = developerToolsHeading()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertRemoteDebugging() = remoteDebuggingToggle()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertExportOuinetLog() = exportOuinetLog()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertExportOuinetLogDescription() = exportOuinetLogDescription()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertExportOuinetLogDownload() = exportOuinetLogDownload()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertExportOuinetLogView() = exportOuinetLogView()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertAnnouncementSource() = announcementSource()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertAnnouncementSourceSummary() = announcementSourceSummary()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertAnnouncementExpiration() = announcementExpiration()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertAnnouncementOption1() = announcementOption1()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertAnnouncementOption2() = announcementOption2()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertAnnouncementOption3() = announcementOption3()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
