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
import ie.equalit.ceno.ui.robots.notificationShade
import ie.equalit.ceno.ui.robots.onboarding

class MediaPlaybackTest {

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
