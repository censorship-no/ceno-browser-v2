package ie.equalit.ceno.settings
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

data class CenoSources(
    val origin: String? = null,
    val injector: String? = null,
    val proxy: String? = null,
    val distCache: String? = null,
    val localCache: String? = null,
)
