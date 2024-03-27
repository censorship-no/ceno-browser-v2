/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings.profilebackup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

interface ProfileBackupController {
    suspend fun getPrefs()
    suspend fun getTopSites()
}

@Suppress("LongParameterList")
class DefaultProfileBackupController(
    private val coroutineContext: CoroutineContext = Dispatchers.Main,
) : ProfileBackupController {

    override suspend fun getPrefs() {
        /*
        withContext(coroutineContext) {
        }
        */
    }

    override suspend fun getTopSites() {
        /*
        withContext(coroutineContext) {
        }
        */
    }
}