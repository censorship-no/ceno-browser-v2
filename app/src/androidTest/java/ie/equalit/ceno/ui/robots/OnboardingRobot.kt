package ie.equalit.ceno.ui.robots

import android.os.Build
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import ie.equalit.ceno.R
import ie.equalit.ceno.helpers.TestAssetHelper
import ie.equalit.ceno.helpers.TestAssetHelper.waitingTime
import ie.equalit.ceno.helpers.TestHelper
import ie.equalit.ceno.helpers.click
import ie.equalit.ceno.settings.Settings

class OnboardingRobot {

    fun verifyStartTooltipExists() = startTooltipExists()

    fun verifyStartTooltipText() = assertStartTooltipText()

    fun verifyStartTooltipButtons() = assertStartTooltipButtons()

    fun verifyPublicPersonalTooltip() = assertPublicPersonalTooltip()

    fun verifyUrlTooltip() = assertUrlTooltip()

    fun verifyShortcutsTooltip() = assertShortcutsTooltip()

    fun verifySourcesTooltip() = assertSourcesTooltip()

    fun verifyClearTooltip() = assertClearTooltip()

    fun beginTooltipsTour() = getStartedButton().click()
    fun verifyExitButton() = assertExitButton()
    fun clickExit() = exitButton().click()
    fun verifyPermissionsTooltip() = assertPermissionsTooltip()

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun givePermissionsIfNeeded(){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getStartedButton().check(matches(withText(R.string.onboarding_battery_button)))
                clickPermissions()
                givePermissions()
            }
        }
        fun skipOnboardingIfNeeded() {
            if (Settings.shouldShowOnboarding(TestHelper.appContext)) {
                skipCenoTourButton().waitForExists(TestAssetHelper.waitingTime)
                skipCenoTourButton().click()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    givePermissionsIfNeeded()
                }
            }
        }
    }
}

fun clickPermissions() = getStartedButton().click()

fun startTooltipExists() {
    onView(withId(R.id.tooltip_overlay_start_layout)).check(matches(isDisplayed()))
}
fun assertExitButton() {
    exitButton().check(matches(isDisplayed()))
}
private fun assertStartTooltipText() {
    onView(withId(R.id.tv_start_tooltip_description)).check(matches(withText(R.string.start_tooltip_description)))
}

private fun assertPermissionsTooltip() {
    onView(withId(R.id.tooltip_overlay_start_layout)).check(matches(isDisplayed()))
    onView(withId(R.id.tv_start_tooltip_title)).check(matches(withText(R.string.onboarding_permissions_title)))
    getStartedButton().check(matches(withText(R.string.onboarding_battery_button)))
}

private fun assertStartTooltipButtons() {
    getStartedButton().check(matches(withText(R.string.onboarding_btn_get_started)))
    skipTourButton().check(matches(isDisplayed()))
}

private fun assertPublicPersonalTooltip() {
    tooltipView().check(matches(isDisplayed()))
    exitButton().check(matches(isDisplayed()))
    tooltipView().check(matches(withContentDescription("Public or Personal?. Use Public tabs for reading news, watching videos and general web-surfing. Share web content with other Ceno users. Personal tabs are better suited for social media accounts, online shopping, and browsing websites you don’t want to share with others.")))
    nextButton().check(matches(isDisplayed()))
}

private fun assertShortcutsTooltip() {
    tooltipView().check(matches(isDisplayed()))
    exitButton().check(matches(isDisplayed()))
    tooltipView().check(matches(withContentDescription("Shortcuts. Save your most visited sites to shortcuts for easy access")))
    nextButton().check(matches(isDisplayed()))
}

private fun assertUrlTooltip() {
    tooltipView().check(matches(isDisplayed()))
    exitButton().check(matches(isDisplayed()))
    tooltipView().check(matches(withContentDescription("Let\'s get browsing!. Type in a website address to start browsing")))
    nextButton().check(matches(isDisplayed()))
}

private fun assertSourcesTooltip() {
    tooltipView().check(matches(isDisplayed()))
    exitButton().check(matches(isDisplayed()))
    tooltipView().check(matches(withContentDescription("Ceno Sources. Easily see where the webpage content is fetched from - directly from the website, via the Ceno network, or Ceno cache")))
    nextButton().check(matches(isDisplayed()))
}

private fun assertClearTooltip() {
    tooltipView().check(matches(isDisplayed()))
    exitButton().check(matches(isDisplayed()))
    tooltipView().check(matches(withContentDescription("Clear everything, \n" +
            "everywhere, all at once. Clear your browser history, credentials and any trace you left on Ceno with one single button.")))
    nextButton().check(matches(isDisplayed()))
}

fun onboarding(interact: OnboardingRobot.() -> Unit): OnboardingRobot.Transition {
    OnboardingRobot().interact()
    return OnboardingRobot.Transition()
}

private fun getStartedButton() = onView(ViewMatchers.withId(R.id.btn_start_ceno_tour))
private fun skipTourButton() = onView(ViewMatchers.withText(R.string.skip_the_tour_button))
private fun exitButton() = onView(withId(R.id.btn_skip_tour))
private fun nextButton() = onView(withId(R.id.btn_next_tooltip))
private fun tooltipView() = onView(withId(R.id.material_target_prompt_view))

private fun skipCenoTourButton() = mDevice.findObject(
    UiSelector().resourceId("${TestHelper.packageName}:id/btn_skip_all_ceno_tour"),
)

fun waitForPermissionsTooltip() {
    mDevice.findObject(
        UiSelector().resourceId("${TestHelper.packageName}:id/btn_start_ceno_tour")).waitForExists(TestAssetHelper.waitingTime)
}
fun clickNext() {
    goToNextTooltipButton().click()
}

fun waitForNextTooltipButton() {
    goToNextTooltipButton().waitForExists(TestAssetHelper.waitingTime)
}

private fun goToNextTooltipButton() = mDevice.findObject(
    UiSelector().resourceId("${TestHelper.packageName}:id/btn_next_tooltip"),
)


private fun permissionAllowButton() = mDevice.findObject(
    UiSelector().resourceId("com.android.permissioncontroller:id/permission_allow_button")
)

private fun permissionDenyButton() = mDevice.findObject(
    UiSelector().resourceId("com.android.permissioncontroller:id/permission_deny_button")
)

private fun backgroundAllowButton() = mDevice.findObject(
UiSelector().resourceId("android:id/button1")
)

private fun backgroundDenyButton() = mDevice.findObject(
    UiSelector().resourceId("android:id/button2")
)


fun givePermissions() {
    //for allowing notifications
    permissionAllowButton().waitForExists(waitingTime)
    permissionAllowButton().click()
    if(backgroundAllowButton().waitForExists(waitingTime)) {
        backgroundAllowButton().click()
    }
}

fun denyPermissions() {
    //for allowing notifications
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissionDenyButton().waitForExists(waitingTime)
        permissionDenyButton().click()
    }
    //for battery optimizations
    if(backgroundDenyButton().waitForExists(waitingTime)) {
        backgroundDenyButton().clickAndWaitForNewWindow(waitingTime)
    }
}
