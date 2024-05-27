package ie.equalit.ceno.ui.robots

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import ie.equalit.ceno.helpers.TestAssetHelper
import ie.equalit.ceno.helpers.TestHelper
import ie.equalit.ceno.settings.Settings

class OnboardingRobot {

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun skipOnboardingIfNeeded() {
            if (Settings.shouldShowOnboarding(TestHelper.appContext)) {
                skipOnboardingButton().waitForExists(TestAssetHelper.waitingTime)
                skipOnboardingButton().click()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    givePermissions()
                }
            }
        }
    }
}

fun onboarding(interact: OnboardingRobot.() -> Unit): OnboardingRobot.Transition {
    OnboardingRobot().interact()
    return OnboardingRobot.Transition()
}

private fun skipOnboardingButton() = mDevice.findObject(
    UiSelector().resourceId("${TestHelper.packageName}:id/btn_onboarding_start_skip"),
)

private fun continuePermissionsButton() = mDevice.findObject(
    UiSelector().resourceId("${TestHelper.packageName}:id/button"),
)


private fun allowButton() = mDevice.findObject(
    UiSelector().text("Allow")
)

fun givePermissions() {
    continuePermissionsButton().waitForExists(TestAssetHelper.waitingTime)
    continuePermissionsButton().click()
    //for allowing notifications
    allowButton().waitForExists(TestAssetHelper.waitingTime)
    allowButton().click()
    //for battery optimizations
    allowButton().waitForExists(TestAssetHelper.waitingTime)
    allowButton().click()
}
