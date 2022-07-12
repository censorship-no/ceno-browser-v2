/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser.home.topsites

import mozilla.components.feature.top.sites.TopSite
import mozilla.components.feature.top.sites.view.TopSitesView
import org.mozilla.reference.browser.ext.ceno.sort
import org.mozilla.reference.browser.utils.CenoPreferences

class DefaultTopSitesView(
    val settings: CenoPreferences
) : TopSitesView {

    override fun displayTopSites(topSites: List<TopSite>) {
        topSites.sort()
    }
}
