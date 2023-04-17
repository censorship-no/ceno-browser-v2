@file:Suppress("DEPRECATION")

package ie.equalit.ceno.ui

import androidx.test.rule.ActivityTestRule
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ie.equalit.ceno.IntentReceiverActivity
import ie.equalit.ceno.helpers.AndroidAssetDispatcher
import ie.equalit.ceno.helpers.BrowserActivityTestRule
import ie.equalit.ceno.helpers.RetryTestRule
import ie.equalit.ceno.helpers.TestAssetHelper
import ie.equalit.ceno.helpers.TestHelper.createCustomTabIntent
import ie.equalit.ceno.ui.robots.customTabScreen

class CustomTabsTest {

    private lateinit var mockWebServer: MockWebServer

    @get:Rule
    val activityTestRule = BrowserActivityTestRule()

    @get: Rule
    val intentReceiverActivityTestRule = ActivityTestRule(
        IntentReceiverActivity::class.java,
        true,
        false,
    )

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
    fun openCustomTabTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
            ),
        )

        customTabScreen {
            verifyCloseButton()
            verifyTrackingProtectionIcon()
            verifySecurityIndicator()
            verifyPageTitle(customTabPage.title)
            verifyPageUrl(customTabPage.url.toString())
            verifyActionButton()
            verifyMenuButton()
        }
    }

    @Test
    fun verifyCustomTabMenuItemsTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
            ),
        )

        customTabScreen {
        }.openMainMenu {
            verifyForwardButton()
            verifyRefreshButton()
            verifyStopButton()
            verifyShareButton()
            verifyRequestDesktopButton()
            verifyFindInPageButton()
            verifyOpenInBrowserButton()
        }
    }

    @Test
    fun customTabNavigationTest() {
        val pageLinks = TestAssetHelper.getGenericAsset(mockWebServer, 4)
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                pageLinks.url.toString(),
            ),
        )

        customTabScreen {
            clickGenericLink("Link 1")
            verifyPageTitle(genericURL.title)
            verifyPageUrl(genericURL.url.toString())
        }.goBack {
            verifyPageTitle(pageLinks.title)
            verifyPageUrl(pageLinks.url.toString())
        }.openMainMenu {
            clickForwardButton()
            verifyPageTitle(genericURL.title)
            verifyPageUrl(genericURL.url.toString())
        }
    }

    @Test
    fun customTabShareTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
            ),
        )

        customTabScreen {
        }.openMainMenu {
        }.clickShareButton {
            verifyShareContentPanel()
        }
    }

    @Test
    fun customTabRequestDesktopSiteTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
            ),
        )

        customTabScreen {
        }.openMainMenu {
            switchRequestDesktopSiteToggle()
        }.openMainMenu {
            verifyRequestDesktopSiteIsTurnedOn()
            switchRequestDesktopSiteToggle()
        }.openMainMenu {
            verifyRequestDesktopSiteIsTurnedOff()
        }
    }

    @Test
    fun customTabOpenInBrowserTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
            ),
        )

        customTabScreen {
        }.openMainMenu {
        }.clickOpenInBrowserButton {
            verifyUrl(customTabPage.url.toString())
        }
    }
}
