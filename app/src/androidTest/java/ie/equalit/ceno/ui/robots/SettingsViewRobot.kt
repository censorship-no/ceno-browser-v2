/* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package ie.equalit.ceno.ui.robots

import android.view.KeyEvent
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers.allOf
import ie.equalit.ceno.R
import ie.equalit.ceno.helpers.*
import ie.equalit.ceno.helpers.TestAssetHelper.waitingTime
import ie.equalit.ceno.helpers.TestHelper.packageName

/**
 * Implementation of Robot Pattern for the settings menu.
 */
class SettingsViewRobot {
    fun verifySettingsViewExists() = assertSettingsView()

    fun verifyNavigateUp(): Unit = assertNavigateUpButton()

    fun verifyGeneralHeading(): ViewInteraction = assertGeneralHeading()
    fun verifyTrackingProtectionButton(): ViewInteraction = assertTrackingProtectionButton()
    fun verifyTrackingProtectionSummary(): ViewInteraction = assertTrackingProtectionSummary()
    fun verifySearchButton(): ViewInteraction = assertSearchButton()
    fun verifySearchSummary(): ViewInteraction = assertSearchSummary()
    fun verifyCustomizationButton(): ViewInteraction = assertCustomizationButton()
    fun verifyCustomizationSummary(): ViewInteraction = assertCustomizationButton()
    fun verifyOpenLinksInApps() = assertOpenLinksInApps()
    fun verifyMakeDefaultBrowserButton() = assertMakeDefaultBrowserButton()
    fun verifyAutofillAppsButton() = assertAutofillAppsButton()
    fun verifyAutofillAppsSummary() = assertAutofillAppsSummary()
    fun verifyDeleteBrowsingData(): ViewInteraction = assertDeleteBrowsingData()
    fun verifyDisableBatteryOptimization(): Unit = assertDisableBatteryOptimizationButton()
    fun verifyShowOnboarding(): ViewInteraction = assertShowOnboarding()

    fun verifySourcesHeading(): ViewInteraction = assertSourcesHeading()
    fun verifyWebsiteCheckbox(): ViewInteraction = assertWebsiteCheckbox()
    fun verifyWebsiteSummary(): ViewInteraction = assertWebsiteSummary()
    fun verifyPrivatelyCheckbox(): ViewInteraction = assertPrivatelyCheckbox()
    fun verifyPrivatelySummary(): ViewInteraction = assertPrivatelySummary()
    fun verifyPubliclyCheckbox(): ViewInteraction = assertPubliclyCheckbox()
    fun verifyPubliclySummary(): ViewInteraction = assertPubliclySummary()
    fun verifySharedCheckbox(): ViewInteraction = assertSharedCheckbox()
    fun verifySharedSummary(): ViewInteraction = assertSharedSummary()

    fun verifyDataHeading(): ViewInteraction = assertDataHeading()
    fun verifyLocalCacheDisplay(): ViewInteraction = assertLocalCacheDisplay()
    fun verifyLocalCacheDefaultValue(): ViewInteraction = assertLocalCacheDefaultValue()
    fun verifyContentsSharedButton(): ViewInteraction = assertContentsSharedButton()
    fun verifyContentsSharedDefaultValue(): ViewInteraction = assertContentsSharedDefaultValue()
    fun verifyClearCachedContentButton(): ViewInteraction = assertClearCachedContentButton()
    fun verifyClearCachedContentSummary(): ViewInteraction = assertClearCachedContentSummary()

    fun verifyDeveloperToolsHeading() = assertDeveloperToolsHeading()
    fun verifyRemoteDebugging() = assertRemoteDebugging()
    fun verifyCustomAddonCollectionButton() = assertCustomAddonCollectionButton()
    fun verifyCenoNetworkDetailsSummary(): ViewInteraction = assertCenoNetworkDetailsSummary()
    fun verifyEnableLogFile(): ViewInteraction = assertEnableLogFile()

    fun verifyAboutHeading() = assertAboutHeading()

    fun verifyCenoBrowserServiceDisplay(): ViewInteraction = assertCenoBrowserServiceDisplay()
    fun verifyGeckoviewVersionDisplay(): ViewInteraction = assertGeckoviewVersionDisplay()
    fun verifyOuinetVersionDisplay(): ViewInteraction = assertOuinetVersionDisplay()
    fun verifyOuinetProtocolDisplay(): ViewInteraction = assertOuinetProtocolDisplay()

    fun verifyAboutEqualitieButton() = assertAboutEqualitieButton()
    fun verifySettingsRecyclerViewToExist() = waitForSettingsRecyclerViewToExist()

    fun clickCustomAddonCollectionButton() = customAddonCollectionButton().click()
    fun verifyCustomAddonCollectionPanelExist() = assertCustomAddonCollectionPanel()

    fun clickOpenLinksInApps() = openLinksInAppsToggle().click()

    fun clickDownRecyclerView(count: Int) {
        for (i in 1..count) {
            recycleView().perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_DOWN))
        }
    }

    // toggleRemoteDebugging does not yet verify that the debug service is started
    // server runs on port 6000
    fun toggleRemoteDebuggingOn() : ViewInteraction {
        //onView(withText("OFF")).check(matches(isDisplayed()))
        remoteDebuggingToggle().assertIsChecked(false)
        remoteDebuggingToggle().click()
        return remoteDebuggingToggle().assertIsChecked(true)
        //return onView(withText("ON")).check(matches(isDisplayed()))
    }

    fun toggleRemoteDebuggingOff() : ViewInteraction {
        remoteDebuggingToggle().assertIsChecked(true)
        remoteDebuggingToggle().click()
        return remoteDebuggingToggle().assertIsChecked(false)
    }

    class Transition {
        /*
        fun openSettingsViewPrivacy(interact: SettingsViewPrivacyRobot.() -> Unit):
                SettingsViewPrivacyRobot.Transition {
            privacyButton().click()
            SettingsViewPrivacyRobot().interact()
            return SettingsViewPrivacyRobot.Transition()
        }
        */

        fun makeDefaultBrowser(interact: ExternalAppsRobot.() -> Unit):
                ExternalAppsRobot.Transition {
            makeDefaultBrowserButton().click()
            ExternalAppsRobot().interact()
            return ExternalAppsRobot.Transition()
        }

        fun clickAutofillAppsButton(interact: ExternalAppsRobot.() -> Unit):
                ExternalAppsRobot.Transition {
            autofillAppsButton().click()
            ExternalAppsRobot().interact()
            return ExternalAppsRobot.Transition()
        }

        fun openAboutReferenceBrowser(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            aboutEqualitieButton().click()
            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun goBack(interact: NavigationToolbarRobot.() -> Unit): NavigationToolbarRobot.Transition {
            val navigateUpButton = mDevice.findObject(UiSelector().descriptionContains("Navigate up"))
            navigateUpButton.clickAndWaitForNewWindow()

            NavigationToolbarRobot().interact()
            return NavigationToolbarRobot.Transition()
        }
    }
}

private fun waitForSettingsRecyclerViewToExist() {
    mDevice.findObject(UiSelector().resourceId("$packageName:id/recycler_view"))
        .waitForExists(
            waitingTime,
        )
}

private fun assertSettingsView() {
    // verify that we are in the correct settings view
    Espresso.onView(withText(R.string.settings))
    Espresso.onView(withText(R.string.preferences_about_page))
}

private fun recycleView() = onView(withId(R.id.recycler_view))
private fun generalHeading() = onView(withText(R.string.general_category))
private fun trackingProtectionButton() = onView(withText(R.string.tracker_category))
private fun trackingProtectionSummary() = onView(withText(R.string.preferences_privacy_summary))
private fun searchButton() = onView(withText(R.string.preference_category_search))
private fun searchSummary() = onView(withText(R.string.preference_search_summary))
private fun customizationButton() = onView(withText(R.string.preferences_customization))
private fun customizationSummary() = onView(withText(R.string.preferences_customization_summary))
private fun openLinksInAppsToggle() = Espresso.onView(allOf(withId(R.id.switchWidget), hasCousin(withText(R.string.open_links_in_apps))))
private fun makeDefaultBrowserButton() = Espresso.onView(withText(R.string.preferences_make_default_browser))
private fun autofillAppsButton() = onView(withText("Autofill apps"))
private fun autofillAppsSummary() = onView(withText("Autofill logins and passwords in other apps"))
private fun deleteBrowsingDataButton() =  onView(withText(R.string.preferences_delete_browsing_data))
private fun showOnboardingToggle() = onView(allOf(withId(R.id.switchWidget), hasCousin(withText(R.string.preferences_show_onboarding))))

private fun sourcesHeading() = onView(withText(R.string.ceno_sources_category))
private fun websiteCheckbox() = onView(allOf(withId(android.R.id.checkbox), hasCousin(withText(R.string.preferences_ceno_sources_origin))))
private fun websiteSummary() = onView(withText(R.string.preferences_ceno_sources_origin_summary))
private fun privatelyCheckbox() = onView(allOf(withId(android.R.id.checkbox), hasCousin(withText(R.string.preferences_ceno_sources_private))))
private fun privatelySummary() = onView(withText(R.string.preferences_ceno_sources_private_summary))
private fun publiclyCheckbox() = onView(allOf(withId(android.R.id.checkbox), hasCousin(withText(R.string.preferences_ceno_sources_public))))
private fun publiclySummary() = onView(withText(R.string.preferences_ceno_sources_public_summary))
private fun sharedCheckbox() = onView(allOf(withId(android.R.id.checkbox), hasCousin(withText(R.string.preferences_ceno_sources_peers))))
private fun sharedSummary() = onView(withText(R.string.preferences_ceno_sources_peers_summary))

private fun dataHeading() = onView(withText(R.string.ceno_data_category))
private fun localCacheDisplay() = onView(withText(R.string.preferences_ceno_cache_size))
private fun localCacheDefaultValue() = onView(withText("O B"))
private fun contentsSharedButton() = onView(withText(R.string.preferences_ceno_groups_count))
private fun contentsSharedDefaultValue() = onView(withText("0 sites"))
private fun clearCachedContentButton() = onView(withText(R.string.preferences_clear_ceno_cache))
private fun clearCachedContentSummary() = onView(withText(R.string.preferences_clear_ceno_cache_summary))

private fun developerToolsHeading() = Espresso.onView(withText(R.string.developer_tools_category))
private fun remoteDebuggingToggle() = Espresso.onView(allOf(withId(R.id.switchWidget), hasCousin(withText(R.string.preferences_remote_debugging))))
private fun customAddonCollectionButton() = onView(withText("Custom Add-on collection"))
private fun cenoNetworkDetailsSummary() = onView(withText(R.string.preferences_ceno_network_config_summary))
private fun enableLogFile() = onView(allOf(withId(R.id.switchWidget), hasCousin(withText(R.string.preferences_ceno_enable_log))))

private fun aboutHeading() = Espresso.onView(withText(R.string.about_category))
private fun cenoBrowserServiceDisplay() = onView(withText(R.string.ceno_notification_title))
private fun geckoviewVersionDisplay() = onView(withText(R.string.preferences_about_geckoview))
private fun ouinetVersionDisplay() = onView(withText(R.string.preferences_about_ouinet))
private fun ouinetProtocolDisplay() = onView(withText(R.string.preferences_about_ouinet_protocol))

private fun aboutEqualitieButton() = Espresso.onView(withText(R.string.preferences_about_page))

private fun assertGeneralHeading() = generalHeading()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertTrackingProtectionButton() = trackingProtectionButton()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertTrackingProtectionSummary() = trackingProtectionSummary()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertSearchButton() = searchButton()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertSearchSummary() = searchSummary()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertCustomizationButton() = customizationButton()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertCustomizationSummary() = customizationSummary()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertNavigateUpButton() {
    mDevice.wait(Until.findObject(By.text("Navigate up")), TestAssetHelper.waitingTimeShort)
}
private fun assertOpenLinksInApps() = openLinksInAppsToggle()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertMakeDefaultBrowserButton() = makeDefaultBrowserButton()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertAutofillAppsButton() = autofillAppsButton()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertAutofillAppsSummary() = autofillAppsSummary()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertDeleteBrowsingData() = deleteBrowsingDataButton()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertDisableBatteryOptimizationButton() {
    mDevice.wait(Until.findObject(By.text("Disable Battery Optimization")), waitingTime)
}
private fun assertShowOnboarding() = showOnboardingToggle()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertSourcesHeading() = sourcesHeading()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertWebsiteCheckbox() = websiteCheckbox()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertWebsiteSummary() = websiteSummary()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertPrivatelyCheckbox() = privatelyCheckbox()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertPrivatelySummary() = privatelySummary()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertPubliclyCheckbox() = publiclyCheckbox()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertPubliclySummary() = publiclySummary()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertSharedCheckbox() = sharedCheckbox()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertSharedSummary() = sharedSummary()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertDataHeading() = dataHeading()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertLocalCacheDisplay() = localCacheDisplay()
private fun assertLocalCacheDefaultValue() = localCacheDefaultValue()
private fun assertContentsSharedButton() = contentsSharedButton()
private fun assertContentsSharedDefaultValue() = contentsSharedDefaultValue()
private fun assertClearCachedContentButton() = clearCachedContentButton()
private fun assertClearCachedContentSummary() = clearCachedContentSummary()

private fun assertDeveloperToolsHeading() = developerToolsHeading()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertRemoteDebugging() = remoteDebuggingToggle()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertCustomAddonCollectionButton() = customAddonCollectionButton()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertCenoNetworkDetailsSummary() = cenoNetworkDetailsSummary()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertEnableLogFile() = enableLogFile()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertAboutHeading() { aboutHeading()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}
private fun assertCenoBrowserServiceDisplay() = cenoBrowserServiceDisplay()
private fun assertGeckoviewVersionDisplay() = geckoviewVersionDisplay()
private fun assertOuinetVersionDisplay() = ouinetVersionDisplay()
private fun assertOuinetProtocolDisplay() = ouinetProtocolDisplay()
private fun assertAboutEqualitieButton() = aboutEqualitieButton()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertCustomAddonCollectionPanel() {
    mDevice.waitForIdle()
    mDevice.findObject(UiSelector().resourceId("$packageName:id/parentPanel"))
        .waitForExists(waitingTime)
    onView(
        allOf(
            withText(R.string.preferences_customize_amo_collection),
            isDescendantOfA(withId(R.id.title_template)),
        ),
    ).check(matches(isCompletelyDisplayed()))
}
