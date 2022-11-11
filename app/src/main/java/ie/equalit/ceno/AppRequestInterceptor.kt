/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import mozilla.components.browser.errorpages.ErrorPages
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import ie.equalit.ceno.browser.CenoHomeFragment
import ie.equalit.ceno.ext.components
import ie.equalit.ceno.tabs.PrivatePage

/**
 * NB, and FIXME: this class is consumed by a 'Core' component group, but itself relies on 'firefoxAccountsFeature'
 * component; this creates a circular dependency, since firefoxAccountsFeature relies on tabsUseCases
 * which in turn needs 'core' itself.
 */
class AppRequestInterceptor(private val context: Context) : RequestInterceptor {
    override fun onLoadRequest(
        engineSession: EngineSession,
        uri: String,
        lastUri: String?,
        hasUserGesture: Boolean,
        isSameDomain: Boolean,
        isRedirect: Boolean,
        isDirectNavigation: Boolean,
        isSubframeRequest: Boolean
    ): RequestInterceptor.InterceptionResponse? {
        return when (uri) {
            "about:privatebrowsing" -> {
                val page = PrivatePage.createPrivateBrowsingPage(context, uri)
                RequestInterceptor.InterceptionResponse.Content(page, encoding = "base64")
            }

            "about:crashes" -> {
                val intent = Intent(context, CrashListActivity::class.java)
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

                RequestInterceptor.InterceptionResponse.Url("about:blank")
            }

            else -> {
                context.components.services.accountsAuthFeature.interceptor.onLoadRequest(
                    engineSession,
                    uri,
                    lastUri,
                    hasUserGesture,
                    isSameDomain,
                    isRedirect,
                    isDirectNavigation,
                    isSubframeRequest
                ) ?: context.components.services.appLinksInterceptor.onLoadRequest(
                    engineSession,
                    uri,
                    lastUri,
                    hasUserGesture,
                    isSameDomain,
                    isRedirect,
                    isDirectNavigation,
                    isSubframeRequest
                )
            }
        }
    }

    override fun onErrorRequest(
        session: EngineSession,
        errorType: ErrorType,
        uri: String?
    ): RequestInterceptor.ErrorResponse {
        /* CENO: Intercept the error page that is loaded for homepage
         * and instead load a blank html page that the home fragment will overlay */
        return if (uri == CenoHomeFragment.ABOUT_HOME) {
            val page = "resource://android/assets/about_home.html"
            RequestInterceptor.ErrorResponse(page)
        } else {
            val errorPage = ErrorPages.createUrlEncodedErrorPage(context, errorType, uri)
            RequestInterceptor.ErrorResponse(errorPage)
        }
    }

    override fun interceptsAppInitiatedRequests() = true
}
