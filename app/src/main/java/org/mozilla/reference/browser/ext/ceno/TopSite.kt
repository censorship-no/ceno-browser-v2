/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser.ext.ceno

import mozilla.components.feature.top.sites.TopSite

/**
 * CENO: Ext to TopSite for CENO Home TopSiteItemViewHolder
 * Returns the type name of the [TopSite].
 */
fun TopSite.name(): String = when (this) {
    is TopSite.Default -> "DEFAULT"
    is TopSite.Frecent -> "FRECENT"
    is TopSite.Pinned -> "PINNED"
    is TopSite.Provided -> "PROVIDED"
}

/**
 * Returns a sorted list of [TopSite]
 */
fun List<TopSite>.sort(): List<TopSite> {
    return this.toMutableList()
}
