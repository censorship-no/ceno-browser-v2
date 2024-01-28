/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ie.equalit.ceno.ext

import android.util.Patterns

/**
 * Replaces the keys with the values with the map provided.
 */
fun String.replace(pairs: Map<String, String>): String {
    var result = this
    pairs.iterator().forEach { (l, r) -> result = result.replace(l, r) }
    return result
}

/**
 * Helper function extracting a-tags from HTML strings
 */
fun String.extractATags(): List<String> {
    val pattern = Regex("""<a[^>]*>.*?</a>""")
    val matches = pattern.findAll(this)
    return matches.map { it.value }.toList()
}

/**
 * Helper function extracting phone numbers from a string
 */
fun String.extractPhoneNumbers(): List<String> {
    val pattern = Regex(Patterns.PHONE.pattern())
    val matches = pattern.findAll(this)
    return matches.map { it.value }.toList()
}

/**
 * Helper function extracting ipv4 from a string
 */
fun String.extractIpv4Addresses(): List<String> {
    val pattern = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""")
    val matches = pattern.findAll(this)
    return matches.map { it.value }.toList()
}

/**
 * Helper function extracting ipv6 from a string
 */
fun String.extractIpv6Addresses(): List<String> {
    val pattern = Regex("""\b(?:(?:[0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:){1,7}:|
        (?:[0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:){1,5}
        (?::[0-9a-fA-F]{1,4}){1,2}|(?:[0-9a-fA-F]{1,4}:){1,4}(?::[0-9a-fA-F]{1,4}){1,3}|
        (?:[0-9a-fA-F]{1,4}:){1,3}(?::[0-9a-fA-F]{1,4}){1,4}|(?:[0-9a-fA-F]{1,4}:){1,2}
        (?::[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((?::[0-9a-fA-F]{1,4}){1,6})|
        :(?:(?::[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(?::[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::
        (?:ffff(?::0{1,4}){0,1}:){0,1}(?:(?:25[0-5]|(?:2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}
        (?:25[0-5]|(?:2[0-4]|1{0,1}[0-9]){0,1}[0-9])|(?:[0-9a-fA-F]{1,4}:){1,4}:
        (?:(?:25[0-5]|(?:2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(?:25[0-5]|(?:2[0-4]|1{0,1}[0-9]){0,1}[0-9]))\b""")
    val matches = pattern.findAll(this)
    return matches.map { it.value }.toList()
}

/**
 * Helper function for getting `url` as well as `content-text` from an HTML a-tag
 */
fun String.getContentFromATag(): Pair<String?, String?> {
    val regex = "<a\\s+href=['\"](.*?)['\"].*?>(.*?)</a>".toRegex()
    val matchResult = regex.find(this)
    val url = matchResult?.groupValues?.getOrNull(1)
    val text = matchResult?.groupValues?.getOrNull(2)
    return Pair(url, text)
}

/**
 * Helper function for getting the size of a String in MB
 */
fun String.getSizeInMB(): Double {
    return toByteArray(Charsets.UTF_8).size.toDouble() / (1024.0 * 1024.0)
}