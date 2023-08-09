package ie.equalit.ceno

import androidx.compose.ui.text.toUpperCase
import mozilla.components.concept.fetch.Header

const val CENO_ICON = "icons/ceno-logo-32.png"
const val CACHE_MAX_ENTRIES = 500
const val OUINET_RESPONSE_VERSION_MIN = 1 // protocol versions accepted
const val OUINET_RESPONSE_VERSION_MAX = 6

// Requests for URLs matching the following regular expressions
// will always be considered private (thus non-cacheable).
val NO_CACHE_URL_REGEXPS = listOf(
    Regex("^https?://(www\\.)?google\\.com/complete/"), // Google Search completion
    Regex("^https?://(www\\.)?duckduckgo\\.com/ac/") // DuckDuckGo Search completion
)

// <https://stackoverflow.com/a/4835406>
val htmlEscapes = mapOf(
    '&' to "&amp;",
    '<' to "&lt;",
    '>' to "&gt;",
    '"' to "&quot;",
    '\'' to "&#039;"
)
//fun escapeHtml(s: String) = s.replace(Regex("[&<>\"']"), { htmlEscapes[it.value] ?: it.value })

fun removeFragmentFromURL(url: String) = url.replace(Regex("#.*$"), "")

fun removeSchemeFromURL(url: String) = url.replace(Regex("^[a-z][-+.0-9a-z]*://i"), "")

fun removeTrailingSlashes(s: String) = s.replace(Regex("/+$"), "")

fun removeLeadingWWW(s: String) = s.replace(Regex("^www\\."), "")

fun isUrlCacheable(url: String) = NO_CACHE_URL_REGEXPS.none { it.matches(url) }

//fun getDhtGroup(e: Docume) : String {
//    // https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/API/webRequest/onBeforeSendHeaders
//    var url = if (e.documentUrl != null) e.documentUrl else e.url
//    if (url == null) return url
//    url = removeFragmentFromURL(url)
//    url = removeSchemeFromURL(url)
//    url = removeTrailingSlashes(url)
//    url = removeLeadingWWW(url)
//    return url
//}

// https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/API/webRequest/onBeforeSendHeaders
//fun onBeforeSendHeaders(e: Any) : Any {
//    if (e.tabId < 0) {
//        return
//    }
//
//    // tabs.get returns a Promise
//    return browser.tabs.get(e.tabId).then { tab ->
//        // The `tab` structure is described here:
//        // https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/API/tabs/Tab
//
//        var is_private = tab.incognito || !isUrlCacheable(e.url)
//        e.requestHeaders.push(mapOf("name" to "X-Ouinet-Private", "value" to (if (is_private) "True" else "False")))
//
//        if (!is_private) {
//            e.requestHeaders.push(mapOf("name" to "X-Ouinet-Group", "value" to getDhtGroup(e)))
//        }
//
//        return mapOf("requestHeaders" to e.requestHeaders)
//    }
//}

//fun redirect403ToHttps(e: Any) : Any {
//    if (e.statusCode == 403 && e.url.startsWith("http:")) {
//        println("Redirecting to HTTPS")
//        var redirect = URL(e.url)
//        redirect.protocol = "https"
//        redirect.port = ""
//        return mapOf("redirectUrl" to redirect.href)
//    }
//}

// Useful links:
// https://github.com/mdn/webextensions-examples/tree/master/http-response
// https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/API/webRequest/onHeadersReceived
// Chrome's webRequest doc is a bit better ATM
// https://developer.chrome.com/extensions/webRequest
var versionError = false
//fun redirectWhenUpdateRequired(e: Any) : Any {
//    if (!versionError) {
//        for (h in e.responseHeaders) {
//            if (h.name.toUpperCase() == "X-OUINET-ERROR") {
//                val ec = h.value.substring(0, 2)
//                if (ec == "0 " || ec == "1 ") {
//                    versionError = true
//                }
//            }
//        }
//    }
//    if (versionError && !isAppStoreUrl(e.url)) {
//        return mapOf(
//            "redirectUrl" to browser.extension.getURL("update-page/index.html")
//        )
//    }
//}

fun isValidProtocolVersion(p: Any) : Boolean {
    val pn = p.toString().toDouble()
    if (pn.isNaN() || pn % 1 > 0) {
        return false
    }
    return (OUINET_RESPONSE_VERSION_MIN <= pn) && (pn <= OUINET_RESPONSE_VERSION_MAX)
}

fun findHeader(headers: Array<Header>, name: String): Any? {
    val name_u = name.uppercase()
    for (h in headers) {
        if (h.name.uppercase() == name_u) {
            return h.value
        }
    }
    return null
}

const val WARN_THROTTLE_MILLIS = 5 * 60 * 1000
var warningLastShownOn = mutableMapOf<String, Long>() // warning string -> time in milliseconds
//fun warnWhenUpdateDetected(e: Any) {
//    var isOuinetMessage = false
//    for (h in e.responseHeaders) {
//        val hn = h.name.toUpperCase()
//        if (hn == "X-OUINET-VERSION" && isValidProtocolVersion(h.value)) {
//            isOuinetMessage = true // hope this comes before other `X-Ouinet-*` headers
//        } else if (isOuinetMessage && hn == "X-OUINET-WARNING") {
//            val hv = h.value
//            // Do not show the same warning if already shown
//            // in the last `WARN_THROTTLE_MILLIS` milliseconds.
//            val now = Date.now()
//            val lastShown = warningLastShownOn[hv] ?: 0
//            if (now - lastShown < WARN_THROTTLE_MILLIS) {
//                continue
//            }
//            warningLastShownOn[hv] = now
//            browser.notifications.create("", mapOf(
//                "type" to "basic",
//                "title" to browser.i18n.getMessage("bgCenoWarning"),
//                "message" to escapeHtml(hv)
//            ))
//        }
//    }
//}

var gOuinetStats = mutableMapOf<Int, MutableMap<String, Int>>()
val gOuinetSources = arrayOf("origin", "proxy", "injector", "dist-cache", "local-cache")

//browser.webNavigation.onBeforeNavigate.addListener { details ->
//    if (details.frameId != 0) return@addListener
//    val tabId = details.tabId
//    gOuinetStats[tabId] = mutableMapOf()
//}

//fun updateCenoStats(e: Tab) {
//    val tabId = e.tabId
//    if (tabId < 0) return
//    var src = findHeader(e.responseHeaders, "X-Ouinet-Source")
//    if (src == null) src = "unknown"
//    if (e.fromCache) src = "local-cache"
//
//    if (gOuinetStats[tabId] == null) gOuinetStats[tabId] = mutableMapOf()
//
//    if (gOuinetStats[tabId][src] == null) {
//        gOuinetStats[tabId][src] = 1
//    } else {
//        gOuinetStats[tabId][src] += 1
//    }
//
//    browser.storage.local.get("stats") { data ->
//        if (data.stats == null) {
//            data.stats = mutableMapOf()
//        }
//        if (data.stats[tabId] == null) {
//            data.stats[tabId] = mutableMapOf()
//        }
//
//        val stats = data.stats[tabId]
//
//        if (gOuinetStats[tabId] == null) return@get
//
//        for (name in gOuinetSources) {
//            val v = gOuinetStats[tabId][name]
//            stats[name] = v ?: 0
//        }
//
//        data.stats[e.tabId] = stats
//        browser.storage.local.set(data)
//    }
//}

val APP_STORES = arrayOf("play.google.com", "paskoocheh.com", "s3.amazonaws.com")
//fun isAppStoreUrl(url: String): Boolean {
//    val hostname = URL(url).hostname
//    return APP_STORES.contains(hostname)
//}

//fun updateOuinetDetailsFromHeaders(e: Any) {
//    if (e.tabId < 0) {
//        return
//    }
//    // Use the URL from the request as the key instead of the URL
//    // from the tab because if there is a redirect the tab URL has not been updated
//    // yet
//    insertCacheEntry(e.tabId, e.url, getOuinetDetails(e.responseHeaders))
//}

const val INJ_TS_RE = """\bts=([0-9]+)\b"""
//fun getOuinetDetails(headers: Array<Any>): MutableMap<String, Any> {
//    val details = mutableMapOf(
//        "isProxied" to false,
//        "injectionTime" to null,
//        "requestTime" to Date.now() / 1000 // seconds
//    )
//    val no_details = details.toMutableMap()
//    var valid_proto = false
//    for (i in 0 until headers.length) {
//        when (headers[i].name.toUpperCase()) {
//            "X-OUINET-VERSION" -> valid_proto = isValidProtocolVersion(headers[i].value)
//            "X-OUINET-INJECTION" -> {
//                details["isProxied"] = true
//                val ts_match = INJ_TS_RE.toRegex().find(headers[i].value)
//                if (ts_match != null) {
//                    details["injectionTime"] = ts_match.groupValues[1].toInt() - 0
//                }
//            }
//        }
//    }
//    return if (valid_proto) details else no_details
//}

//fun insertCacheEntry(tabId: Int, url: String, details: MutableMap<String, Any>) {
//    browser.storage.local.get("cache") { data ->
//        if (data.cache == null) {
//            data.cache = mutableMapOf()
//        }
//        if (data.cache[tabId] == null) {
//            data.cache[tabId] = mutableMapOf()
//        }
//        if (size(data.cache[tabId]) >= CACHE_MAX_ENTRIES) {
//            removeOldestEntries(data.cache[tabId])
//        }
//        data.cache[tabId][url] = details
//        // Store an entry for the origin as well because single-page-apps,
//        // change the URL without causing requests.
//        data.cache[tabId][URL(url).origin] = details
//        browser.storage.local.set(data)
//    }
//}

fun removeOldestEntries(entries: Any) {
    val array = Object.entries(entries)
    array.sortWith(Comparator { (k1, v1), (k2, v2) -> v1.requestTime - v2.requestTime })
    var i = 0
    while (size(entries) > CACHE_MAX_ENTRIES) {
        entries.remove(array[i++][0])
    }
}

fun size(o: Any): Int {
    return Object.keys(o).length
}

fun setPageActionIcon(tabId: Int, isUsingOuinet: Boolean) {
    if (isUsingOuinet) {
        browser.pageAction.show(tabId)
    } else {
        browser.pageAction.hide(tabId)
    }
}

/**
 * Updates the icon for the page action using the details
 * about the page from local storage.
 */
fun setPageActionForTab(tabId: Int) {
    getCacheEntry(tabId) { ouinetDetails ->
        val isUsingOuinet = ouinetDetails != null && ouinetDetails.isProxied
        setPageActionIcon(tabId, true /* isUsingOuinet */)
    }
}

fun getCacheEntry(tabId: Int, callback: (Any?) -> Unit): Any? {
    return browser.storage.local.get("cache") { data ->
        if (data.cache == null || data.cache[tabId] == null) {
            callback(null)
            return
        }
        browser.tabs.get(tabId).then { tab ->
            val fromUrl = data.cache[tabId][tab.url]
            if (fromUrl != null) {
                callback(fromUrl)
                return
            }
            val origin = URL(tab.url).origin
            callback(data.cache[tabId][origin])
        }
    }
}

/**
 * Remove entries from local storage when tab is removed.
 */
fun removeCacheForTab(tabId: Int) {
    browser.storage.local.get("cache") { data ->
        if (data.cache == null) {
            return
        }
        // Remove all entries for the tab.
        data.cache.remove(tabId)
        browser.storage.local.set(data)
    }
}

fun clearLocalStorage() {
    browser.storage.local.get("cache") { data ->
        if (data.cache == null) {
            return
        }
        browser.tabs.query({}).then { tabs ->
            val tabIds = tabs.map { tab -> tab.id }
            for (key in Object.keys(data.cache)) {
                if (!tabIds.contains(key)) {
                    data.cache.remove(key)
                }
            }
            browser.storage.local.set(data)
        }
    }
}

/**
 * Configure the Ouinet client as a proxy.
 *
 * As of 2022-02-22, and according
 * to @url{https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/API/proxy/settings},
 * this only works on Desktop Firefox >= 60.
 */
fun setOuinetClientAsProxy() {
    val proxyEndpoint = "${config.ouinet_client.host}:${config.ouinet_client.proxy.port}"
    browser.proxy.settings.set(value = mapOf(
        "proxyType" to "manual",
        "http" to proxyEndpoint,
        "ssl" to proxyEndpoint
    )).then {
        println("Ouinet client configured as proxy for HTTP and HTTPS.")
    }.catch { e ->
        // This does not work on Android:
        // check occurrences of "proxy.settings is not supported on android"
        // in `gecko-dev/toolkit/components/extensions/parent/ext-proxy.js`.
        println("Failed to configure HTTP and HTTPS proxies: $e")
    }
}

//setOuinetClientAsProxy()
//
//browser.browserAction.onClicked.addListener {
//    val url = browser.extension.getURL("settings.html")
//    browser.tabs.create(url = url)
//}
//
//browser.webRequest.onBeforeSendHeaders.addListener(
//onBeforeSendHeaders,
//mapOf("urls" to listOf("<all_urls>")),
//listOf("blocking", "requestHeaders")
//)
//
//browser.webRequest.onHeadersReceived.addListener(
//redirect403ToHttps,
//mapOf("urls" to listOf("<all_urls>")),
//listOf("blocking", "responseHeaders")
//)
//
//browser.webRequest.onHeadersReceived.addListener(
//redirectWhenUpdateRequired,
//mapOf("urls" to listOf("<all_urls>")),
//listOf("blocking", "responseHeaders")
//)
//
//browser.webRequest.onHeadersReceived.addListener(
//warnWhenUpdateDetected,
//mapOf("urls" to listOf("<all_urls>")),
//listOf("responseHeaders")
//)
//
//browser.webRequest.onHeadersReceived.addListener(
//updateCenoStats,
//mapOf("urls" to listOf("<all_urls>")),
//listOf("responseHeaders")
//)
//
//browser.webRequest.onHeadersReceived.addListener(
//updateOuinetDetailsFromHeaders,
//mapOf("urls" to listOf("<all_urls>")),
//listOf("responseHeaders")
//)
//
//browser.runtime.onMessage.addListener { request, sender, sendResponse ->
//    setPageActionForTab(sender.tab.id, sender)
//}
//
//browser.runtime.onStartup.addListener { clearLocalStorage() }
//
///**
// * Each time a tab is updated, reset the page action for that tab.
// */
//browser.tabs.onUpdated.addListener { id, changeInfo, tab ->
//    setPageActionForTab(id)
//}
//
//browser.tabs.onRemoved.addListener { id ->
//    removeCacheForTab(id)
//}
//
//browser.pageAction.onClicked.addListener { browser.pageAction.openPopup() }