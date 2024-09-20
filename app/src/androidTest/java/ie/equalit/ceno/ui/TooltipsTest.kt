package ie.equalit.ceno.ui

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import ie.equalit.ceno.R
import ie.equalit.ceno.helpers.AndroidAssetDispatcher
import ie.equalit.ceno.helpers.BrowserActivityTestRule
import ie.equalit.ceno.helpers.RetryTestRule
import ie.equalit.ceno.helpers.TestAssetHelper
import ie.equalit.ceno.helpers.TestHelper
import ie.equalit.ceno.ui.robots.OnboardingRobot
import ie.equalit.ceno.ui.robots.clickNext
import ie.equalit.ceno.ui.robots.clickPermissions
import ie.equalit.ceno.ui.robots.givePermissions
import ie.equalit.ceno.ui.robots.navigationToolbar
import ie.equalit.ceno.ui.robots.onboarding
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class TooltipsTest {
    private val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
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
        mDevice.wait(Until.gone(By.res("${TestHelper.packageName}:id/iv_standby_logo")), TestAssetHelper.waitingTime)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun allTooltipsTest() {
        val genericOneURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        onboarding {
            verifyStartTooltipExists()
            verifyStartTooltipText()
            verifyStartTooltipButtons()
            beginTooltipsTour()
            verifyPublicPersonalTooltip()
            clickNext()
            verifyShortcutsTooltip()
            clickNext()
            verifyUrlTooltip()
        }
        navigationToolbar {
        }.enterUrlAndEnterToBrowser(genericOneURL.url) {
        }
        onboarding {
            verifySourcesTooltip()
            clickNext()
            verifyClearTooltip()
            clickNext()
            verifyPermissionsTooltip()
        }.givePermissionsIfNeeded()
    }

    @Test
    fun skipAllTooltips() {
        onboarding {
        }.skipOnboardingIfNeeded()
    }

    @Test
    fun exitTooltip() {
        onboarding {
            verifyStartTooltipExists()
            verifyStartTooltipButtons()
            beginTooltipsTour()
            verifyPublicPersonalTooltip()
            verifyExitButton()
            clickExit()
            verifyPermissionsTooltip()
        }.givePermissionsIfNeeded()
    }

}