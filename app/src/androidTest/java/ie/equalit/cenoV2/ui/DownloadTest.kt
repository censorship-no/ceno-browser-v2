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
import ie.equalit.cenoV2.ui.robots.downloadRobot
import ie.equalit.cenoV2.ui.robots.navigationToolbar
import ie.equalit.cenoV2.ui.robots.notificationShade

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
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

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
}
