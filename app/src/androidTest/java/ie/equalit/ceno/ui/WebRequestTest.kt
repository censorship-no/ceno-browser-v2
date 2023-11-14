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
import ie.equalit.ceno.helpers.TestAssetHelper
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

    private fun navigateToSourcesAndSet(website: Boolean, private: Boolean, public: Boolean, shared : Boolean){
        navigationToolbar {
        }.openThreeDotMenu {
        }.openSettings {
            Thread.sleep(5000)
            clickDownRecyclerView(18)
            Thread.sleep(5000)
            verifyWebsiteSourcesButton()
            verifyWebsiteSourcesSummary()
        }.openSettingsViewSources {
            setWebsiteSources(website, private, public, shared)
        }.goBack {
        }.goBack {
        }
    }

    private fun verifyWebRequestList(shouldSucceed : Boolean) {
        var displayUrl = ""
        for (scenario in BuildConfig.SCENARIO_ARRAY) {
            scenario["website"]?.let {
                displayUrl = it.replaceFirst("^https?://(www\\.)?".toRegex(), "")
            }
            if (scenario["personal"].toBoolean()) {
                navigationToolbar {
                }.openTabTrayMenu {
                    openPrivateBrowsing()
                }.openNewTab {
                }
            }
            else {
                navigationToolbar {
                }.openTabTrayMenu {
                    openRegularBrowsing()
                }.openNewTab {
                }
            }
            navigationToolbar {
            }.enterUrlAndEnterToBrowser(displayUrl.toUri()) {
                verifyUrl(displayUrl)
                verifyPageLoaded()
                if (shouldSucceed) {
                    scenario["expectedText"]?.let { verifyPageContent(it) }
                } else {
                    verifyPageContent("Failed to retrieve the resource (after attempting all configured mechanisms)")
                }
                // TODO: how to check for success?
            }
        }
    }

    // Test names are binary-encoded decimal,
    @Test
    fun webRequestListSourcesTest0() {
        navigateToSourcesAndSet(website = false, private = false, public = false, shared = false)
        verifyWebRequestList(shouldSucceed = false)
    }
    @Test
    fun webRequestListSourcesTest1() {
        navigateToSourcesAndSet(website = false, private = false, public = false, shared = true)
        verifyWebRequestList(shouldSucceed = false)
    }
    @Test
    fun webRequestListSourceTest2() {
        navigateToSourcesAndSet(website = false, private = false, public = true, shared = false)
        verifyWebRequestList(shouldSucceed = false)
    }
    @Test
    fun webRequestListSourceTest3() {
        navigateToSourcesAndSet(website = false, private = false, public = true, shared = true)
        verifyWebRequestList(shouldSucceed = false)
    }
    @Test
    fun webRequestListSourceTest4() {
        navigateToSourcesAndSet(website = false, private = true, public = false, shared = false)
        verifyWebRequestList(shouldSucceed = false)
    }
    @Test
    fun webRequestListSourceTest5() {
        navigateToSourcesAndSet(website = false, private = true, public = false, shared = true)
        verifyWebRequestList(shouldSucceed = false)
    }
    @Test
    fun webRequestListSourceTest6() {
        navigateToSourcesAndSet(website = false, private = true, public = true, shared = false)
        verifyWebRequestList(shouldSucceed = false)
    }
    @Test
    fun webRequestListSourceTest7() {
        navigateToSourcesAndSet(website = false, private = true, public = true, shared = true)
        verifyWebRequestList(shouldSucceed = false)
    }
    @Test
    fun webRequestListSourceTest8() {
        navigateToSourcesAndSet(website = true, private = false, public = false, shared = false)
        verifyWebRequestList(shouldSucceed = true)
    }
}
