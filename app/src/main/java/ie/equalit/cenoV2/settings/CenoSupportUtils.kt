/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.cenoV2.settings

import android.content.Context
import mozilla.components.support.ktx.android.content.appVersionName
//import org.mozilla.fenix.settings.account.AuthIntentReceiverActivity
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.Locale

object CenoSupportUtils {
    /* TODO: these should just be set using the resource files... */
    const val CENO_URL = "https://censorship.no/user-manual/"
    const val WIKIPEDIA_URL = "https://www.wikipedia.org/"
    const val APNEWS_URL = "https://apnews.com/"
    const val REUTERS_URL = "https://www.reuters.com/"
    const val BBC_URL = "https://www.bbc.com/news"

    const val CENO_ES_URL = "https://censorship.no/user-manual/es/"
    const val ELPAIS_URL = "https://elpais.com/internacional/"
    const val INFOBAE_URL = "https://www.infobae.com/america/"
    const val BBC_ES_URL = "https://www.bbc.com/mundo"

    const val CENO_FA_URL = "https://censorship.no/user-manual/fa/"
    const val PASKOOCEH_URL = "http://paskoocheh.com/"
    const val FACTNAMEH_URL = "https://factnameh.com/fa"
    const val BBC_FA_URL = "https://www.bbc.com/persian"

    const val COURRIER_URL = "https://www.courrierinternational.com/"
    const val LAPRESSE_URL = "https://www.lapresse.ca/"
    const val BBC_FR_URL = "https://www.bbc.com/"

    const val CENO_MY_URL = "https://censorship.no/user-manual/my/"
    const val MYNOW_URL = "https://myanmar-now.org/mm"
    const val JUSTICEMY_URL = "https://justiceformyanmar.org/"
    const val BBC_MY_URL = "https://www.bbc.com/burmese"

    const val CENO_RU_URL = "https://censorship.no/user-manual/ru/"
    const val MEDUZA_URL = "https://meduza.io/"
    const val MEDIAZONA_URL = "https://zona.media/"
    const val BBC_RU_URL = "https://www.bbc.com/russian"

    const val CENO_UK_URL = "https://censorship.no/user-manual/uk/"
    const val PRAVDA_URL = "https://www.pravda.com.ua/"
    const val HROMADSKE_URL = "https://hromadske.ua/"
    const val BBC_UK_URL = "https://www.bbc.com/ukrainian"

    const val LTN_URL = "https://www.ltn.com.tw/"
    const val TWREPORTER_URL = "https://www.twreporter.org/"
    const val BBC_ZH_URL = "https://www.bbc.com/zhongwen/simp"

    enum class SumoTopic(internal val topicStr: String) {
        HELP("faq-android"),
        PRIVATE_BROWSING_MYTHS("common-myths-about-private-browsing"),
        YOUR_RIGHTS("your-rights"),
        TRACKING_PROTECTION("tracking-protection-firefox-android"),
        WHATS_NEW("whats-new-firefox-preview"),
        OPT_OUT_STUDIES("how-opt-out-studies-firefox-android"),
        SEND_TABS("send-tab-preview"),
        SET_AS_DEFAULT_BROWSER("set-firefox-preview-default"),
        SEARCH_SUGGESTION("how-search-firefox-preview"),
        CUSTOM_SEARCH_ENGINES("custom-search-engines"),
        SYNC_SETUP("how-set-firefox-sync-firefox-android"),
        QR_CAMERA_ACCESS("qr-camera-access"),
        SMARTBLOCK("smartblock-enhanced-tracking-protection"),
        SPONSOR_PRIVACY("sponsor-privacy"),
        HTTPS_ONLY_MODE("https-only-mode-firefox-android"),
    }

    enum class MozillaPage(internal val path: String) {
        PRIVATE_NOTICE("privacy/firefox/"),
        MANIFESTO("about/manifesto/")
    }

    /**
     * Gets a support page URL for the corresponding topic.
     */
    fun getSumoURLForTopic(
        context: Context,
        topic: SumoTopic,
        locale: Locale = Locale.getDefault()
    ): String {
        val escapedTopic = getEncodedTopicUTF8(topic.topicStr)
        // Remove the whitespace so a search is not triggered:
        val appVersion = context.appVersionName?.replace(" ", "")
        val osTarget = "Android"
        val langTag = getLanguageTag(locale)
        return "https://support.mozilla.org/1/mobile/$appVersion/$osTarget/$langTag/$escapedTopic"
    }

    /**
     * Gets a support page URL for the corresponding topic.
     * Used when the app version and os are not part of the URL.
     */
    fun getGenericSumoURLForTopic(topic: SumoTopic, locale: Locale = Locale.getDefault()): String {
        val escapedTopic = getEncodedTopicUTF8(topic.topicStr)
        val langTag = getLanguageTag(locale)
        return "https://support.mozilla.org/$langTag/kb/$escapedTopic"
    }

    fun getFirefoxAccountSumoUrl(): String {
        return "https://support.mozilla.org/kb/access-mozilla-services-firefox-account"
    }

    fun getMozillaPageUrl(page: MozillaPage, locale: Locale = Locale.getDefault()): String {
        val path = page.path
        val langTag = getLanguageTag(locale)
        return "https://www.mozilla.org/$langTag/$path"
    }

    fun getWhatsNewUrl(context: Context) = getSumoURLForTopic(context, SumoTopic.WHATS_NEW)

    /*
    fun createCustomTabIntent(context: Context, url: String): Intent = CustomTabsIntent.Builder()
        .setInstantAppsEnabled(false)
        .setDefaultColorSchemeParams(
            CustomTabColorSchemeParams.Builder().setToolbarColor(context.getColorFromAttr(R.attr.layer1)).build()
        )
        .build()
        .intent
        .setData(url.toUri())
        .setClassName(context, IntentReceiverActivity::class.java.name)
        .setPackage(context.packageName)

     */

    /*
    fun createAuthCustomTabIntent(context: Context, url: String): Intent =
        createCustomTabIntent(context, url).setClassName(context, AuthIntentReceiverActivity::class.java.name)
     */

    private fun getEncodedTopicUTF8(topic: String): String {
        try {
            return URLEncoder.encode(topic, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException("utf-8 should always be available", e)
        }
    }

    private fun getLanguageTag(locale: Locale): String {
        val language = locale.language
        val country = locale.country // Can be an empty string.
        return if (country.isEmpty()) language else "$language-$country"
    }
}
