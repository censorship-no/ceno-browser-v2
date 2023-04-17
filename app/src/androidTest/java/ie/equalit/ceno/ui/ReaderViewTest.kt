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

class ReaderViewTest {

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
    fun verifyReaderViewDetectionTest() {
        val readerViewPage = TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(readerViewPage.url) {
        }
        navigationToolbar {
            verifyReaderViewButton()
        }.clickReaderViewButton {
            verifyAppearanceButtonExists()
            clickAppearanceButton()
            verifyAppearanceMenuExists()
        }.dismissAppearanceMenu {
        }
        navigationToolbar {
        }.clickReaderViewButton {
            verifyAppearanceButtonDoesntExists()
        }
    }

    @Test
    fun readerViewFontChangeTest() {
        val readerViewPage = TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(readerViewPage.url) {
        }
        navigationToolbar {
            verifyReaderViewButton()
        }.clickReaderViewButton {
            verifyAppearanceButtonExists()
            clickAppearanceButton()
            verifyAppearanceMenuExists()
            verifyFontGroupButtons()
            clickSansSerifButton()
            verifyActiveAppearanceFont("SANSSERIF")
            clickSerifButton()
            verifyActiveAppearanceFont("SERIF")
        }
    }

    @Test
    fun readerViewFontSizeChangeTest() {
        val readerViewPage = TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(readerViewPage.url) {
        }
        navigationToolbar {
            verifyReaderViewButton()
        }.clickReaderViewButton {
            verifyAppearanceButtonExists()
            clickAppearanceButton()
            verifyAppearanceMenuExists()
            verifyIncreaseFontSizeButton()
            verifyDecreaseFontSizeButton()
            verifyAppearanceFontSize(3)
            clickIncreaseFontSizeButton()
            verifyAppearanceFontSize(4)
            clickDecreaseFontSizeButton()
            verifyAppearanceFontSize(3)
        }
    }

    @Test
    fun readerViewColorSchemeChangeTest() {
        val readerViewPage = TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterUrlAndEnterToBrowser(readerViewPage.url) {
        }
        navigationToolbar {
            verifyReaderViewButton()
        }.clickReaderViewButton {
            verifyAppearanceButtonExists()
            clickAppearanceButton()
            verifyAppearanceMenuExists()
            verifyColorSchemeGroupButtons()
            clickSepiaColorButton()
            verifyAppearanceColorScheme("SEPIA")
            clickDarkColorButton()
            verifyAppearanceColorScheme("DARK")
            clickLightColorButton()
            verifyAppearanceColorScheme("LIGHT")
        }
    }
}
