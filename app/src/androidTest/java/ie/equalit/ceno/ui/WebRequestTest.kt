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
import ie.equalit.ceno.ui.robots.standby

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
        standby {
        }.waitForStandbyIfNeeded()
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
            clickDownRecyclerView(20)
            Thread.sleep(5000)
            verifyWebsiteSourcesButton()
            verifyWebsiteSourcesSummary()
        }.openSettingsViewSources {
            setWebsiteSources(website, private, public, shared)
        }.goBack {
        }.goBack {
        }
    }

    private fun verifyWebRequestList(
        scenarios: MutableList<MutableMap<String, String>>,
        personal: Boolean
    ) {
        var displayUrl = ""
        for (s in scenarios) {
            s["website"]?.let {
                displayUrl = it.replaceFirst("^https?://(www\\.)?".toRegex(), "")
            }
            if (personal) {
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
                s["expectedText"]?.let { verifyPageContent(it) }
            }
        }
    }

    @Test
    fun webRequestListSourcesTest() {
        val config = BuildConfig.CONFIG_MAP
        val scenarioList = BuildConfig.SCENARIO_LIST
        navigateToSourcesAndSet(
            website = config["website"].toBoolean(),
            private = config["private"].toBoolean(),
            public = config["public"].toBoolean(),
            shared = config["shared"].toBoolean()
        )
        verifyWebRequestList(scenarioList, config["personalTab"].toBoolean())
    }
}
