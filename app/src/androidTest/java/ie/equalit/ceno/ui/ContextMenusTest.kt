/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.ui

import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ie.equalit.ceno.helpers.AndroidAssetDispatcher
import ie.equalit.ceno.helpers.BrowserActivityTestRule
import ie.equalit.ceno.helpers.RetryTestRule
import ie.equalit.ceno.helpers.TestAssetHelper
import ie.equalit.ceno.ui.robots.browser
import ie.equalit.ceno.ui.robots.navigationToolbar
import ie.equalit.ceno.ui.robots.onboarding

class ContextMenusTest {

    private lateinit var mockWebServer: MockWebServer

    @get:Rule
    val activityTestRule = BrowserActivityTestRule()

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
        onboarding {
        }.skipOnboardingIfNeeded()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun verifyLinkContextMenuItems() {
        val pageLinks = TestAssetHelper.getGenericAsset(mockWebServer, 4)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(pageLinks.url) {
            longClickMatchingText("Link 1")
            verifyLinkContextMenuItems()
        }
    }

    @Test
    fun openLinkInNewTabTest() {
        val pageLinks = TestAssetHelper.getGenericAsset(mockWebServer, 4)
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(pageLinks.url) {
            longClickMatchingText("Link 1")
            clickContextOpenLinkInNewTab()
            clickSnackbarSwitchButton()
        }
        navigationToolbar {
        }.openTabTrayMenu {
            verifyRegularBrowsingTab()
            verifyExistingOpenTabs(pageLinks.title)
            verifyExistingOpenTabs(genericURL.title)
        }
    }

    @Test
    fun openLinkInPrivateTabTest() {
        val pageLinks = TestAssetHelper.getGenericAsset(mockWebServer, 4)
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(pageLinks.url) {
            longClickMatchingText("Link 1")
            clickContextOpenLinkInPrivateTab()
            clickSnackbarSwitchButton()
        }
        navigationToolbar {
        }.openTabTrayMenu {
            openPrivateBrowsing()
            verifyPrivateBrowsingTab()
            verifyExistingOpenTabs(genericURL.title)
        }
    }

    @Test
    fun contextCopyLinkTest() {
        val pageLinks = TestAssetHelper.getGenericAsset(mockWebServer, 4)
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(pageLinks.url) {
            longClickMatchingText("Link 1")
            clickContextCopyLink()
            waitUntilCopyLinkSnackbarIsGone()
        }
        navigationToolbar {
        }.clickToolbar {
            pasteAndLoadCopiedLink()
        }

        browser {
            verifyUrl(genericURL.displayUrl)
        }
    }

    @Test
    fun contextShareLinkTest() {
        val pageLinks = TestAssetHelper.getGenericAsset(mockWebServer, 4)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(pageLinks.url) {
            longClickMatchingText("Link 1")
        }.clickContextShareLink {
            verifyShareContentPanel()
        }
    }

    @Test
    fun copyTextTest() {
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(genericURL.url) {
            longClickAndCopyText("content")
        }
        navigationToolbar {
        }.clickToolbar {
            clickClearToolbarButton()
            longClickToolbar()
            clickPasteText()
            verifyPastedToolbarText("content")
        }
    }

    @Test
    fun selectAllAndCopyTextTest() {
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(genericURL.url) {
            longClickAndCopyText("content", true)
        }
        navigationToolbar {
        }.clickToolbar {
            clickClearToolbarButton()
            longClickToolbar()
            clickPasteText()
            verifyPastedToolbarText("Page content: 1")
        }
    }
}
