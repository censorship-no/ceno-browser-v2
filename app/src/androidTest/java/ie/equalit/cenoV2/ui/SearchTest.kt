/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.cenoV2.ui

import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ie.equalit.cenoV2.helpers.AndroidAssetDispatcher
import ie.equalit.cenoV2.helpers.BrowserActivityTestRule
import ie.equalit.cenoV2.helpers.RetryTestRule
import ie.equalit.cenoV2.helpers.TestAssetHelper
import ie.equalit.cenoV2.ui.robots.navigationToolbar

class SearchTest {

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
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun siteSearchSuggestionTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(defaultWebPage.url) {
        }
        navigationToolbar {
        }.openTabTrayMenu {
        }.openNewTab {
        }.clickToolbar {
            typeText("generic1.html")
            verifySearchSuggestion(defaultWebPage.title)
        }.clickSearchSuggestion(defaultWebPage.title) {
            verifyUrl(defaultWebPage.url.toString())
        }
    }
}
