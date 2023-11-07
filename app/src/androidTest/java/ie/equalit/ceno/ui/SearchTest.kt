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
import ie.equalit.ceno.ui.robots.navigationToolbar
import ie.equalit.ceno.ui.robots.onboarding

class SearchTest {

    private lateinit var mockWebServer: MockWebServer

    @get:Rule
    val activityTestRule = BrowserActivityTestRule()

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(1)

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
    fun siteSearchSuggestionTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        // Disable suggestions from search engine provide
        // because they can cause this test to fail
        navigationToolbar {
        }.openThreeDotMenu {
        }.openSettings {
            verifySearchButton()
        }.openSettingsViewSearch {
            verifyGetSearchSuggestionsToggle()
            toggleGetSearchSuggestions()
        }.goBack {
        }.goBack {
        }
        navigationToolbar {
        }.enterUrlAndEnterToBrowser(defaultWebPage.url) {
        }
        navigationToolbar {
        }.openTabTrayMenu {
            verifyExistingOpenTabs(defaultWebPage.title)
        }.openNewTab {
        }.clickToolbar {
            typeText("generic1.html")
            verifySearchSuggestion(defaultWebPage.title)
        }.clickSearchSuggestion(defaultWebPage.title) {
            verifyUrl(defaultWebPage.displayUrl)
        }
    }
}
