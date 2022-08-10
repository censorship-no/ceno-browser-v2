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
import ie.equalit.cenoV2.ui.robots.notificationShade

class MediaPlaybackTest {

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
    fun audioPlaybackTest() {
        val audioTestPage = TestAssetHelper.getAudioPageAsset(mockWebServer)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(audioTestPage.url) {
            verifyMediaPlayerControlButtonState("Play")
            clickMediaPlayerControlButton("Play")
        }

        notificationShade {
            verifySystemMediaNotificationExists(audioTestPage.title)
            verifySystemMediaNotificationControlButtonState("Pause")
            clickSystemMediaNotificationControlButton("Pause")
            verifySystemMediaNotificationControlButtonState("Play")
        }.closeNotification {}
    }

    @Test
    fun videoPlaybackTest() {
        val videoTestPage = TestAssetHelper.getVideoPageAsset(mockWebServer)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(videoTestPage.url) {
            clickMediaPlayerControlButton("Play")
        }

        notificationShade {
            verifySystemMediaNotificationExists(videoTestPage.title)
            verifySystemMediaNotificationControlButtonState("Pause")
            clickSystemMediaNotificationControlButton("Pause")
            verifySystemMediaNotificationControlButtonState("Play")
        }.closeNotification {}
    }

    @Test
    fun hiddenVideoControlsContextMenuTest() {
        val noControlsVideoTestPage = TestAssetHelper.getNoControlsVideoPageAsset(mockWebServer)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(noControlsVideoTestPage.url) {
            longClickMatchingText("test_link_video")
            verifyNoControlsVideoContextMenuItems()
        }
    }
}
