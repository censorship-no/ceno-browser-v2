/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.components

import android.content.Context
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.feature.accounts.FirefoxAccountsAuthFeature
import mozilla.components.feature.app.links.AppLinksInterceptor
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.service.fxa.manager.FxaAccountManager
import ie.equalit.ceno.R
import ie.equalit.ceno.settings.CustomPreferenceManager

/**
 * Component group which encapsulates foreground-friendly services.
 */
class Services(
    private val context: Context,
    private val accountManager: FxaAccountManager,
    private val tabsUseCases: TabsUseCases,
) {
    val accountsAuthFeature by lazy {
        FirefoxAccountsAuthFeature(
            accountManager,
            redirectUrl = BackgroundServices.REDIRECT_URL,
        ) {
                _, authUrl ->
            MainScope().launch {
                tabsUseCases.addTab.invoke(authUrl)
            }
        }
    }

    val appLinksInterceptor by lazy {
        AppLinksInterceptor(
            context,
            interceptLinkClicks = true,
            launchInApp = {
                CustomPreferenceManager.getBoolean(context, R.string.pref_key_launch_external_app)
            },
        )
    }
}
