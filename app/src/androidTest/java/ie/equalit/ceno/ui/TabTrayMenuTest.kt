/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ie.equalit.ceno.R
import ie.equalit.ceno.helpers.AndroidAssetDispatcher
import ie.equalit.ceno.helpers.BrowserActivityTestRule
import ie.equalit.ceno.helpers.RetryTestRule
import ie.equalit.ceno.helpers.TestAssetHelper
import ie.equalit.ceno.helpers.click
import ie.equalit.ceno.ui.robots.mDevice
import ie.equalit.ceno.ui.robots.navigationToolbar
import ie.equalit.ceno.ui.robots.onboarding
import ie.equalit.ceno.ui.robots.standby

/**
 *  Tests for verifying tab tray menu:
 * - Appears when counter tabs is clicked
 * - Expected options are displayed as listed below
 * - Options/Buttons in this menu work as expected
 */

class TabTrayMenuTest {

    private lateinit var mockWebServer: MockWebServer

    @get:Rule val activityTestRule = BrowserActivityTestRule()

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(1)

    // SetUp to close all tabs before starting each test
    @Before
    fun setUp() {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }

        onboarding {
        }.skipOnboardingIfNeeded()

        standby {
        }.waitForStandbyIfNeeded()

        fun optionsButton() = onView(ViewMatchers.withContentDescription("More options"))
        fun closeAllTabsButton() = onView(ViewMatchers.withText("Close All Tabs"))
        fun goBackButton() = onView(ViewMatchers.withContentDescription("back"))
        val tabCounterButton = onView(withId(R.id.counter_text))

        mDevice.waitForIdle()
        tabCounterButton.click()

        val thereAreTabsOpenInTabTray = mDevice.findObject(UiSelector().text("about:blank")).exists()

        if (thereAreTabsOpenInTabTray) {
            optionsButton().click()
            closeAllTabsButton().click()
        } else {
            goBackButton().click()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.
    // This test verifies the tab tray menu items are all in place
    @Test
    fun tabTrayUITest() {
        navigationToolbar {
        }.openTabTrayMenu {
            verifyRegularBrowsingTab()
            verifyPrivateBrowsingTab()
            verifyGoBackButton()
            verifyNewTabButton()
        }.openMoreOptionsMenu(activityTestRule.activity) {
            verifyCloseAllTabsButton()
        }
    }

    // This test verifies that close all tabs option works as expected
    @Test
    fun closeAllTabsTest() {
        val genericOneURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val genericFourURL = TestAssetHelper.getGenericAsset(mockWebServer, 4)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(genericOneURL.url) {
            verifyUrl(genericOneURL.displayUrl)
        }
        navigationToolbar {
        }.openTabTrayMenu {
        }.openNewTab {
        }.enterUrlAndEnterToBrowser(genericFourURL.url) {
            verifyUrl(genericFourURL.displayUrl)
        }
        navigationToolbar {
            checkNumberOfTabsTabCounter("2")
        }.openTabTrayMenu {
            verifyExistingOpenTabs(genericOneURL.title)
            verifyExistingOpenTabs(genericFourURL.title)
        }.openMoreOptionsMenu(activityTestRule.activity) {
            mDevice.waitForIdle()
            verifyCloseAllTabsButton()
        }.closeAllTabs {
            verifyNoTabAddressView()
            checkNumberOfTabsTabCounter("0")
        }
    }

    // This test verifies that close all tabs option works as expected
    @Test
    fun closeAllPrivateTabsTest() {
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.openTabTrayMenu {
            openPrivateBrowsing()
        }.openNewTab {
        }.enterUrlAndEnterToBrowser(genericURL.url) {
            verifyPageContent("Page content: 1")
        }
        navigationToolbar {
        }.openTabTrayMenu {
            //openPrivateBrowsing()
            verifyThereIsOnePrivateTabOpen()
        }.openMoreOptionsMenu(activityTestRule.activity) {
            mDevice.waitForIdle()
            verifyCloseAllPrivateTabsButton()
        }.closeAllPrivateTabs {
        }.openTabTrayMenu {
            openPrivateBrowsing()
            verifyThereAreNotPrivateTabsOpen()
            //TODO: going back sometimes fails due to bug that takes user back to homepage too soon
            //goBackFromTabTrayTest()
        }
    }

    @Test
    fun closeOneTabXButtonTest() {
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(genericURL.url) {
            verifyUrl(genericURL.displayUrl)
        }
        navigationToolbar {
            checkNumberOfTabsTabCounter("1")
        }.openTabTrayMenu {
            verifyExistingOpenTabs(genericURL.title)
        }.closeTabXButton(genericURL.title) {
        }
        navigationToolbar {
            checkNumberOfTabsTabCounter("0")
            verifyNewTabAddressView("Search or enter address")
        }
    }

    @Test
    fun openTwoTabsCloseOneTabXButtonTest() {
        val genericOneURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val genericFourURL = TestAssetHelper.getGenericAsset(mockWebServer, 4)
        fun goBackButton() = onView(ViewMatchers.withContentDescription("back"))

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(genericOneURL.url) {
            Thread.sleep(5000)
            verifyUrl(genericOneURL.displayUrl)
        }
        navigationToolbar {
        }.openTabTrayMenu {
        }.openNewTab {
        }.enterUrlAndEnterToBrowser(genericFourURL.url) {
            Thread.sleep(5000)
            verifyUrl(genericFourURL.displayUrl)
        }
        navigationToolbar {
            checkNumberOfTabsTabCounter("2")
        }.openTabTrayMenu {
            verifyExistingOpenTabs(genericOneURL.title)
            verifyExistingOpenTabs(genericFourURL.title)
        }.closeTabXButton(genericOneURL.title) {
        }
        goBackButton().click()
        navigationToolbar {
            checkNumberOfTabsTabCounter("1")
        }
    }

    // This test verifies the change between regular-private browsing works
    @Test
    fun privateRegularModeChangeTest() {
        navigationToolbar {
        }.openTabTrayMenu {
            openPrivateBrowsing()
            verifyPrivateBrowsingTab(true)
            verifyRegularBrowsingTab(false)
            openRegularBrowsing()
            verifyPrivateBrowsingTab(false)
            verifyRegularBrowsingTab(true)
            goBackFromTabTrayTest()
        }
    }

    // This test verifies the new tab is open and that its items are all in place
    @Test
    fun openNewTabTest() {
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.openTabTrayMenu {
        }.openNewTab {
            verifyNewTabAddressView("Search or enter address")
            checkNumberOfTabsTabCounter("0")
        }.openTabTrayMenu {
        }.openNewTab {
        }.enterUrlAndEnterToBrowser(genericURL.url) {
            verifyUrl(genericURL.displayUrl)
        }
        navigationToolbar {
            checkNumberOfTabsTabCounter("1")
        }.openTabTrayMenu {
            verifyExistingOpenTabs(genericURL.title)
        }.clickOpenTab(genericURL.title) {
            verifyUrl(genericURL.displayUrl)
        }
    }

    // This test verifies the new tab is open and that its items are all in place
    @Test
    fun openNewPrivateTabTest() {
        val firstGenericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondGenericURL = TestAssetHelper.getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.openTabTrayMenu {
            openPrivateBrowsing()
        }.openNewTab {
            verifyNewTabAddressView("Search or enter address")
            checkNumberOfTabsTabCounter("0")
        }.openTabTrayMenu {
            openPrivateBrowsing()
        }.openNewTab {
        }.enterUrlAndEnterToBrowser(firstGenericURL.url) {
            verifyPageContent("Page content: 1")
        }
        navigationToolbar {
            checkNumberOfTabsTabCounter("1")
        }.openTabTrayMenu {
            openPrivateBrowsing()
            verifyExistingOpenTabs(firstGenericURL.title)
        }.openNewTab {
        }.enterUrlAndEnterToBrowser(secondGenericURL.url) {
            verifyPageContent("Page content: 2")
        }
        navigationToolbar {
            checkNumberOfTabsTabCounter("2")
        }
    }

    // This test verifies the back button functionality
    @Test
    fun goBackFromTabTrayTest() {
        navigationToolbar {
        }.openTabTrayMenu {
        }.goBackFromTabTray {
            // For now checking new tab is valid, this will change when browsing to/from different places
            verifyNoTabAddressView()
        }
    }
}
