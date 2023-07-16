/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno

import android.net.Uri
import androidx.fragment.app.Fragment
import mozilla.components.concept.engine.manifest.WebAppManifest
import mozilla.components.feature.pwa.ext.getWebAppManifest
import ie.equalit.ceno.browser.ExternalAppBrowserFragment
import java.util.UUID

/**
 * Activity that holds the BrowserFragment that is launched within an external app,
 * such as custom tabs and progressive web apps.
 */
class ExternalAppBrowserActivity : BrowserActivity() {
    override fun createBrowserFragment(sessionId: String?) {
//        if (sessionId != null) {
//            val manifest = intent.getWebAppManifest()
//            val scope = when (manifest?.display) {
//                WebAppManifest.DisplayMode.FULLSCREEN,
//                WebAppManifest.DisplayMode.STANDALONE,
//                -> Uri.parse(manifest.scope ?: manifest.startUrl)
//
//                WebAppManifest.DisplayMode.MINIMAL_UI,
//                WebAppManifest.DisplayMode.BROWSER,
//                -> null
//                else -> null
//            }
//
//            return ExternalAppBrowserFragment.create(
//                sessionId ?: UUID.randomUUID().toString(),
//                manifest,
//                listOfNotNull(scope),
//            )
//        } else {
            // Fall back to browser fragment
            super.createBrowserFragment(sessionId)
//        }
    }
}
