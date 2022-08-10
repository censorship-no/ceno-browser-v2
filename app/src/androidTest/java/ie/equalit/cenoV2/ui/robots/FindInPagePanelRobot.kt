/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.cenoV2.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.uiautomator.UiSelector
import org.hamcrest.Matchers.not
import org.junit.Assert.assertTrue
import ie.equalit.cenoV2.R
import ie.equalit.cenoV2.helpers.TestAssetHelper.waitingTime
import ie.equalit.cenoV2.helpers.TestHelper.packageName
import ie.equalit.cenoV2.helpers.TestHelper.waitForObjects
import ie.equalit.cenoV2.browser.helpers.click

/**
 * Implementation of Robot Pattern for the FindInPage Panel.
 */
class FindInPagePanelRobot {

    fun verifyFindInPageBar() = assertFindInPageBarExists()
    fun verifyFindInPageBarIsDismissed() = assertFindInPageBarIsDismissed()
    fun clickFindInPageNextButton() = findInPageNextButton().click()
    fun clickFindInPagePreviousButton() = findInPagePreviousButton().click()
    fun clickFindInPageCloseButton() = findInPageCloseButton().click()

    fun enterFindInPageQuery(expectedText: String) {
        val findInPageQuery = mDevice.findObject(UiSelector().resourceId("$packageName:id/find_in_page_query_text"))
        findInPageQuery.setText(expectedText)
    }

    fun verifyFindInPageResult(ratioCounter: String) {
        mDevice.findObject(UiSelector().resourceId("$packageName:id/find_in_page_result_text"))
            .waitForExists(waitingTime)
        assertTrue(mDevice.findObject(UiSelector().textContains(ratioCounter)).waitForExists(waitingTime))
    }

    class Transition {
        fun findInPage(interact: FindInPagePanelRobot.() -> Unit): Transition {
            return Transition()
        }
    }
}

private fun findInPageBar() = onView(withId(R.id.findInPageBar))
private fun findInPageNextButton() = onView(withId(R.id.find_in_page_next_btn))
private fun findInPagePreviousButton() = onView(withId(R.id.find_in_page_prev_btn))
private fun findInPageCloseButton() = onView(withId(R.id.find_in_page_close_btn))

private fun assertFindInPageBarExists() {
    mDevice.waitForObjects(mDevice.findObject(UiSelector().resourceId("$packageName:id/findInPageBar")))
    findInPageBar().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    findInPagePreviousButton().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    findInPageNextButton().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    findInPageCloseButton().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun assertFindInPageBarIsDismissed() {
    mDevice.findObject(UiSelector().resourceId("$packageName:id/findInPageBar")).waitUntilGone(waitingTime)
    findInPageBar().check(matches(not(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))))
    findInPagePreviousButton().check(matches(not(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))))
    findInPageNextButton().check(matches(not(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))))
    findInPageCloseButton().check(matches(not(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))))
}
