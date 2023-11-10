/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.ui

import androidx.core.net.toUri
import ie.equalit.ceno.BuildConfig
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ie.equalit.ceno.helpers.AndroidAssetDispatcher
import ie.equalit.ceno.helpers.BrowserActivityTestRule
import ie.equalit.ceno.helpers.RetryTestRule
import ie.equalit.ceno.ui.robots.navigationToolbar
import ie.equalit.ceno.ui.robots.onboarding

class WebRequestTest {

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
    fun webRequestNoSourcesTest() {
        val testUrl = "https://example.com"
        navigationToolbar {
        }.openThreeDotMenu {
        }.openSettings {
            Thread.sleep(5000)
            clickDownRecyclerView(18)
            Thread.sleep(5000)
            verifyWebsiteSourcesButton()
            verifyWebsiteSourcesSummary()
        }.openSettingsViewSources {
            setWebsiteSources(
                website = false,
                private = false,
                public = false,
                shared = false
            )
        }.goBack {
        }.goBack {
        }
        navigationToolbar {
        }.enterUrlAndEnterToBrowser(testUrl.toUri()) {
            verifyPageContent("Failed to retrieve the resource (after attempting all configured mechanisms)")
        }
    }

    @Test
    fun webRequestListNoSourcesTest() {
        val testUrlList = BuildConfig.URL_ARRAY
        navigationToolbar {
        }.openThreeDotMenu {
        }.openSettings {
            Thread.sleep(5000)
            clickDownRecyclerView(18)
            Thread.sleep(5000)
            verifyWebsiteSourcesButton()
            verifyWebsiteSourcesSummary()
        }.openSettingsViewSources {
            setWebsiteSources(
                website = false,
                private = false,
                public = false,
                shared = false
            )
        }.goBack {
        }.goBack {
        }
        for (url in testUrlList) {
            navigationToolbar {
            }.openTabTrayMenu {
            }.openNewTab {
            }.enterUrlAndEnterToBrowser(url.toUri()) {
                verifyPageContent("Failed to retrieve the resource (after attempting all configured mechanisms)")
            }
        }
    }
}
