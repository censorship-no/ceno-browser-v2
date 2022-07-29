/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser.components.ceno.appstate

import mozilla.components.feature.top.sites.TopSite
import mozilla.components.lib.state.State

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
) : State
