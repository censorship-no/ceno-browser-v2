package ie.equalit.ceno.ui

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
import org.junit.Ignore

@Ignore("Disabled - will probably remove/disable Add-ons menu soon")
class AddonsTest {

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
    fun addonsListingPageTest() {
        navigationToolbar {
        }.openThreeDotMenu {
        }.openAddonsManager {
            verifyAddonsRecommendedView()
        }
    }

    /*
    @Test
    fun cancelAddonInstallTest() {
        navigationToolbar {
        }.openThreeDotMenu {
        }.openAddonsManager {
            clickInstallAddonButton(defaultAddOnName)
            verifyInstallAddonPrompt(defaultAddOnName)
            clickCancelInstallButton()
            verifyAddonsRecommendedView()
        }
    }

    @Test
    fun installAddonTest() {
        navigationToolbar {
        }.openThreeDotMenu {
        }.openAddonsManager {
            clickInstallAddonButton(defaultAddOnName)
            verifyInstallAddonPrompt(defaultAddOnName)
            clickAllowInstallAddonButton()
            waitForAddonDownloadComplete()
            verifyAddonDownloadCompletedPrompt(defaultAddOnName)
        }
    }

    @Test
    fun verifyAddonElementsTest() {
        navigationToolbar {
        }.openThreeDotMenu {
        }.openAddonsManager {
            verifyAddonsRecommendedView()
            clickInstallAddonButton(defaultAddOnName)
            clickAllowInstallAddonButton()
            waitForAddonDownloadComplete()
            dismissAddonDownloadCompletedPrompt(defaultAddOnName)
            openAddon(defaultAddOnName)
            verifyAddonElementsView(defaultAddOnName)
        }
    }

    @Test
    fun removeAddonTest() {
        navigationToolbar {
        }.openThreeDotMenu {
        }.openAddonsManager {
            verifyAddonsRecommendedView()
            clickInstallAddonButton(defaultAddOnName)
            clickAllowInstallAddonButton()
            waitForAddonDownloadComplete()
            dismissAddonDownloadCompletedPrompt(defaultAddOnName)
            openAddon(defaultAddOnName)
            clickRemoveAddonButton()
            verifyAddonsRecommendedView()
        }
    }
     */
}
