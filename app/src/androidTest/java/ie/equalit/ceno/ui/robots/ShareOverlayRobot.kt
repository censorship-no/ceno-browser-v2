package ie.equalit.ceno.ui.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import ie.equalit.ceno.R
import ie.equalit.ceno.helpers.click

class ShareOverlayRobot {

    // This function verifies the share layout when a single tab is shared - no tab info shown
    fun verifyShareTabLayout() = assertShareTabLayout()
    fun verifyRecentAppsContainer() = assertRecentAppsContainer()
    fun verifyShareApps() = assertShareApps()

    fun verifyRecentAppsContainerHeader() = assertRecentAppsContainerHeader()
    fun verifyShareAppsHeader() = assertShareAppsHeader()

    fun verifyShareToPdf() = assertShareToPdf()
    private fun assertShareTabLayout() =
        onView(ViewMatchers.withId(R.id.sharingLayout))
            .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

    private fun assertRecentAppsContainer() =
        onView(ViewMatchers.withId(R.id.recentAppsContainer))
            .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    private fun assertShareApps() =
        onView(ViewMatchers.withId(R.id.appsShareLayout))
            .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    private fun assertRecentAppsContainerHeader() =
        onView(ViewMatchers.withText(R.string.share_link_recent_apps_subheader))
            .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

    private fun assertShareAppsHeader() =
        onView(ViewMatchers.withText(R.string.share_link_all_apps_subheader))
            .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))


    private fun assertShareToPdf() = shareToPdfButton()
        .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

    class Transition {
        fun clickSaveAsPDF(interact: DownloadRobot.() -> Unit): DownloadRobot.Transition {
            shareToPdfButton().click()

            DownloadRobot().interact()
            return DownloadRobot.Transition()
        }
    }
}
fun shareToPdfButton() = onView(ViewMatchers.withId(R.id.save_pdf))

fun shareOverlay(interact: ShareOverlayRobot.() -> Unit): ShareOverlayRobot.Transition {
    ShareOverlayRobot().interact()
    return ShareOverlayRobot.Transition()
}