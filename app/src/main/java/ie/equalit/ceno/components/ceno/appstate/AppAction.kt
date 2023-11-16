/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.components.ceno.appstate

import ie.equalit.ceno.browser.BrowsingMode
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.lib.state.Action
import ie.equalit.ceno.components.ceno.AppStore
import ie.equalit.ceno.home.CenoMessageCard

/** CENO: Ported from Fenix, significantly stripped down
 *  since TopSites is the only currently supported AppState
 * [Action] implementation related to [AppStore].
 */
sealed class AppAction : Action {

    data class Change(
        val topSites: List<TopSite>,
        val showCenoModeItem: Boolean,
        val showThanksCard: Boolean
    ) : AppAction()

    data class TopSitesChange(val topSites: List<TopSite>) : AppAction()
    object RemoveCenoModeItem : AppAction()

    data class RemoveThanksCard(val showThanksCard: Boolean) : AppAction()

    data class ModeChange(val mode: BrowsingMode) : AppAction()
}
