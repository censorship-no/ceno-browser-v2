/* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.ui.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import ie.equalit.ceno.R
import ie.equalit.ceno.helpers.TestAssetHelper

/**
 * Implementation of Robot Pattern for the settings privacy menu.
 */
class SettingsViewPrivacyRobot {

    fun verifyPrivacyUpButton() = assertPrivacyUpButton()
    fun verifyTrackingProtectionHeading() = assertTrackingProtectionHeading()
    fun verifyTPEnableInNormalBrowsing() = assertTpEnableInNormalBrowsing()
    fun verifyTPEnableinPrivateBrowsing() = assertTpEnableInPrivateBrowsing()

    class Transition {
        fun settingsViewPrivacy(interact: SettingsViewPrivacyRobot.() -> Unit): Transition {
            return Transition()
        }
    }
}

private fun trackingProtectionHeading() = Espresso.onView(ViewMatchers.withText(R.string.tracker_category))
private fun tpEnableInNormalBrowsing() = Espresso.onView(ViewMatchers.withText(R.string.preferences_tracking_protection_normal))
private fun tpEnableInPrivateBrowsing() = Espresso.onView(ViewMatchers.withText(R.string.preferences_tracking_protection_private))
private fun assertPrivacyUpButton() {
    mDevice.wait(Until.findObject(By.text("Navigate up")), TestAssetHelper.waitingTimeShort)
}
private fun assertTrackingProtectionHeading() = trackingProtectionHeading()
    .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertTpEnableInNormalBrowsing() = tpEnableInNormalBrowsing()
    .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertTpEnableInPrivateBrowsing() = tpEnableInPrivateBrowsing()
    .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
