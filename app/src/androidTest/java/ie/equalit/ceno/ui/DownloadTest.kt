package ie.equalit.ceno.ui

import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import ie.equalit.ceno.helpers.AndroidAssetDispatcher
import ie.equalit.ceno.helpers.BrowserActivityTestRule
import ie.equalit.ceno.helpers.RetryTestRule
import ie.equalit.ceno.helpers.TestAssetHelper
import ie.equalit.ceno.ui.robots.downloadRobot
import ie.equalit.ceno.ui.robots.navigationToolbar
import ie.equalit.ceno.ui.robots.notificationShade
import ie.equalit.ceno.ui.robots.onboarding

class DownloadTest {

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

    @Ignore("Disabled - Some versions of android request permission to allow access to files")
    @Test
    fun cancelFileDownloadTest() {
        val downloadPage = TestAssetHelper.getDownloadAsset(mockWebServer)
        val downloadFileName = "web_icon.png"

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(downloadPage.url) {}

        downloadRobot {
            cancelDownload()
        }

        notificationShade {
            verifyDownloadNotificationDoesNotExist("Download completed", downloadFileName)
        }.closeNotification {}
    }

    @Ignore("Disabled - https://github.com/mozilla-mobile/reference-browser/issues/2130")
    @Test
    fun fileDownloadTest() {
        val downloadPage = TestAssetHelper.getDownloadAsset(mockWebServer)
        val downloadFileName = "web_icon.png"

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(downloadPage.url) {}

        downloadRobot {
            confirmDownload()
        }

        notificationShade {
            verifyDownloadNotificationExist("Download completed", downloadFileName)
        }.closeNotification {}
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2048448
    // Save edited PDF file from the share overlay
    @Test
    fun saveAsPdfFunctionalityTest() {
        val genericURL =
            TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser (genericURL.url) {
            verifyPageContent("Page content: 1")
        }
        navigationToolbar {
        }.openThreeDotMenu {
        }.clickShareButton {
        }.clickSaveAsPDF {
            if (allowButtonExists()) {
                clickAllow()
            }
            verifyDownloadPrompt("Page content: 1")
        }.clickDownload ()
    }
}
