package ie.equalit.ceno.ui.robots

import android.util.Log
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import ie.equalit.ceno.R
import junit.framework.Assert.assertTrue
import ie.equalit.ceno.helpers.TestAssetHelper.waitingTime
import ie.equalit.ceno.helpers.TestHelper.getPermissionAllowID
import ie.equalit.ceno.helpers.TestHelper.packageName
import ie.equalit.ceno.helpers.click

class DownloadRobot {
    fun cancelDownload() {
        closeDownloadButton.waitForExists(waitingTime)
        closeDownloadButton.click()
    }

    fun confirmDownload() {
        downloadButton.waitForExists(waitingTime)
        downloadButton.click()
    }

    fun allowButtonExists() : Boolean {
        return allowButton.waitForExists(waitingTime)
    }

    fun clickAllow() {
        clickAllowButton()
    }

    fun verifyDownloadPrompt(filename:String) {
        Espresso.onView(ViewMatchers.withText("Download"))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    class Transition {
        fun clickDownload() {
            downloadButton().click()
        }
    }
}
private fun downloadButton() =
    Espresso.onView(ViewMatchers.withId(R.id.download_button))
        .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

fun downloadRobot(interact: DownloadRobot.() -> Unit): DownloadRobot.Transition {
    DownloadRobot().interact()
    return DownloadRobot.Transition()
}

private fun assertDownloadPopup() {
    mDevice.waitForIdle()
    assertTrue(
        mDevice.findObject(UiSelector().resourceId("$packageName:id/filename"))
            .waitForExists(waitingTime),
    )
}

private fun clickAllowButton() {
    mDevice.waitForIdle()
    mDevice.wait(
        Until.findObject(
            By.res(getPermissionAllowID() + ":id/permission_message"),
        ),
        waitingTime,
    )
    mDevice.wait(
        Until.findObject(
            By.res(getPermissionAllowID() + ":id/permission_allow_button"),
        ),
        waitingTime,
    )

    allowButton.click()
}

private val closeDownloadButton = mDevice.findObject(UiSelector().resourceId("$packageName:id/close_button"))
private val downloadButton = mDevice.findObject(UiSelector().resourceId("$packageName:id/download_button"))
private val allowButton = mDevice.findObject(UiSelector().resourceId(getPermissionAllowID() + ":id/permission_allow_button"))
