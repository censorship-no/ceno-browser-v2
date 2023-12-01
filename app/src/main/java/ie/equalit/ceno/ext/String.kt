/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ie.equalit.ceno.ext

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
 * Helper function for getting `url` as well as `content-text` from an HTML a-tag
 */
fun String.getContentFromATag(): Pair<String?, String?> {
    val regex = "<a\\s+href=['\"](.*?)['\"].*?>(.*?)</a>".toRegex()
    val matchResult = regex.find(this)
    val url = matchResult?.groupValues?.getOrNull(1)
    val text = matchResult?.groupValues?.getOrNull(2)
    return Pair(url, text)
}