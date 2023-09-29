/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings

/*
    This is a simple interface that can be used to listen for success
    or error states when an API call is made via Ouinet
 */
interface OuinetResponseListener {

    fun onBTChangeSuccess(source: String)
    fun onErrorResponse()
}