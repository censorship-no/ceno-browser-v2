/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.components.ceno.appstate

import ie.equalit.ceno.browser.BrowsingMode
import ie.equalit.ouinet.Ouinet.RunningState
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.lib.state.State
import org.json.JSONObject

/** CENO: Ported from Fenix, significantly stripped down
 *  since TopSites is the only currently supported AppState
 *
 * Value type that represents the state of the tabs tray.
 *
 * @property topSites The list of [TopSite] in the [HomeFragment].
 *
 */
data class AppState(
    val topSites: List<TopSite> = emptyList(),
    val mode: BrowsingMode = BrowsingMode.Normal,
    val ouinetStatus: RunningState = RunningState.Started,
    val showBridgeCard: Boolean = true,
    val sourceCounts: MutableMap<String, JSONObject?> = mutableMapOf()
) : State
