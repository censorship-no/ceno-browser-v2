/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.ui.robots

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import junit.framework.Assert.assertTrue
import ie.equalit.ceno.helpers.TestAssetHelper.waitingTime
import ie.equalit.ceno.helpers.TestAssetHelper.waitingTimeShort
import ie.equalit.ceno.helpers.TestHelper.packageName

/**
 * Implementation of Robot Pattern for any non-Reference Browser (external) apps.
 */
class ExternalAppsRobot {
    fun verifyAndroidDefaultApps() = assertDefaultAppsLayout()
    fun verifyAndroidAutofillServices() = assertAutofillServices()
    fun verifyYouTubeApp() = assertYouTubeApp()

    class Transition {
        fun externalApps(interact: ExternalAppsRobot.() -> Unit): Transition {
            return Transition()
        }
    }
}

private fun assertDefaultAppsLayout() {
    mDevice.wait(Until.findObject(By.text("Default apps")), waitingTimeShort)
}

private fun assertAutofillServices() {
    mDevice.waitForWindowUpdate(packageName, waitingTime)
    assertTrue(
        mDevice.findObject(UiSelector().textContains("Autofill service"))
            .waitForExists(waitingTime),
    )
}

@Suppress("SwallowedException")
private fun assertYouTubeApp() {
    try {
        // Check youtube's home buttons
        mDevice.waitForIdle()
        assertTrue(
            mDevice.findObject(UiSelector().text("Home"))
                .waitForExists(waitingTime),
        )
        assertTrue(
            mDevice.findObject(UiSelector().text("Subscriptions"))
                .waitForExists(waitingTime),
        )
    } catch (e: AssertionError) {
        println("The native youtube app opens but needs to be updated")
        // In case the app isn't up to date on the emulator an update message will be displayed
        assertTrue(
            mDevice.findObject(UiSelector().text("Update for a faster, better YouTube"))
                .waitForExists(waitingTime),
        )
    }
}
