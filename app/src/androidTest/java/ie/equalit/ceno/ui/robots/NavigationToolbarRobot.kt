/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.ui.robots

import android.net.Uri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import ie.equalit.ceno.R
import ie.equalit.ceno.ext.waitAndInteract
import ie.equalit.ceno.helpers.TestAssetHelper.waitingTime
import ie.equalit.ceno.helpers.TestHelper.packageName
import ie.equalit.ceno.helpers.click
import org.mozilla.fenix.ui.robots.ReaderViewRobot

/**
 * Implementation of Robot Pattern for the navigation toolbar menu.
 */
class NavigationToolbarRobot {

    fun verifyNoTabAddressView() = assertNoTabAddressText()
    fun verifyNewTabAddressView(url: String) = assertNewTabAddressText(url)
    fun verifyReaderViewButton() = assertReaderViewButton()
    fun checkNumberOfTabsTabCounter(numTabs: String) = numberOfOpenTabsTabCounter.check(matches(withText(numTabs)))

    class Transition {

        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun enterUrlAndEnterToBrowser(url: Uri, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            urlBar().click()
            mDevice.findObject(
                UiSelector()
                    .textContains("Search or enter address"),
            ).waitForExists(waitingTime)
            awesomeBar().setText(url.toString())
            mDevice.pressEnter()

            mDevice.findObject(
                UiSelector()
                    .resourceId("$packageName:id/mozac_browser_toolbar_progress"),
            ).waitUntilGone(waitingTime)

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun openThreeDotMenu(interact: ThreeDotMenuRobot.() -> Unit): ThreeDotMenuRobot.Transition {
            /* TODO: A short wait is required in case HTTPS-by-default is not finished installing yet */
            mDevice.findObject(
                UiSelector()
                    .resourceId("$packageName:id/mozac_browser_toolbar_menu"),
            )
                .waitForExists(waitingTime)
            threeDotMenuButton().click()

            ThreeDotMenuRobot().interact()
            return ThreeDotMenuRobot.Transition()
        }

        fun openTabTrayMenu(interact: TabTrayMenuRobot.() -> Unit): TabTrayMenuRobot.Transition {
            openTabTray().click()
            TabTrayMenuRobot().interact()
            return TabTrayMenuRobot.Transition()
        }

        fun clickToolbar(interact: AwesomeBarRobot.() -> Unit): AwesomeBarRobot.Transition {
            urlBar().click()
            mDevice.waitForIdle()
            mDevice.findObject(UiSelector().textContains("Search or enter address"))
                .waitForExists(waitingTime)
            AwesomeBarRobot().interact()
            return AwesomeBarRobot.Transition()
        }

        fun clickReaderViewButton(interact: ReaderViewRobot.() -> Unit): ReaderViewRobot.Transition {
            readerViewButton().click()
            mDevice.waitForWindowUpdate(packageName, waitingTime)
            ReaderViewRobot().interact()
            return ReaderViewRobot.Transition()
        }

        // TODO: this should return Robot for testings the content sources sheet
        fun  openContentSourcesSheet(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.findObject(
                UiSelector()
                    .resourceId("$packageName:id/mozac_browser_toolbar_tracking_protection_indicator"),
            ).waitForExists(waitingTime)
            contentSourcesButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

fun navigationToolbar(interact: NavigationToolbarRobot.() -> Unit): NavigationToolbarRobot.Transition {
    NavigationToolbarRobot().interact()
    return NavigationToolbarRobot.Transition()
}

private fun openTabTray() = mDevice.findObject(UiSelector().resourceId("$packageName:id/tab_counter_box"))
private var numberOfOpenTabsTabCounter = onView(withId(R.id.counter_text))
private fun urlBar() =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_origin_view"))
private fun awesomeBar() =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view"))
private fun threeDotMenuButton() = onView(withId(R.id.mozac_browser_toolbar_menu))
private fun readerViewButton() = onView(withId(R.id.mozac_browser_toolbar_page_actions))

private fun contentSourcesButton() = onView(withId(R.id.mozac_browser_toolbar_tracking_protection_indicator))

private fun assertNoTabAddressText() {
    mDevice.waitAndInteract(Until.findObject(By.text("Search or enter address"))) {}
}

private fun assertNewTabAddressText(url: String) {
    mDevice.waitAndInteract(Until.findObject(By.textContains(url))) {}
}

private fun assertReaderViewButton() {
    mDevice.waitForWindowUpdate(packageName, waitingTime)
    mDevice.findObject(
        UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_page_actions"),
    ).waitForExists(waitingTime)

    readerViewButton().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}
