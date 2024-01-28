/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.utils

import android.util.Log
import android.util.Patterns
import ie.equalit.ceno.ext.extractIpv4Addresses
import ie.equalit.ceno.ext.extractIpv6Addresses
import ie.equalit.ceno.ext.extractPhoneNumbers
import ie.equalit.ceno.ext.getSizeInMB
import ie.equalit.ceno.settings.SettingsFragment
import java.net.Inet4Address
import java.net.InetAddress

object LogReader {

    fun getLogEntries(filterInSeconds: Double): List<String> {
        val logs = mutableListOf<String>()

        try {
            val process = Runtime.getRuntime().exec("logcat -d *:D | grep -v 'chatty'")
            val reader = process.inputStream.bufferedReader()

            Log.d("PPPPPP", "Ze filter is $filterInSeconds")

            var line: String? = reader.readLine()
            while (line != null && logs.joinToString("\n").getSizeInMB() < SettingsFragment.LOG_FILE_SIZE_LIMIT_MB) {
                logs.add(scrubLogs(line))
                line = reader.readLine()
            }
        } catch (e: Exception) {
            Log.e("LogReader", "Error reading logs", e)
        }

        return logs
    }

    private fun scrubLogs(originalLogs: String): String {

        /*
        Scrubbing:

        Phone number
        Email addresses
        Mac addresses
        Ipv4 INET address
        Ipv6 INET address
        */

        // Define patterns for PIIs
        val emailPattern = Patterns.EMAIL_ADDRESS.toRegex()
        val macAddressPattern = "([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}".toRegex()

        var formattedLogs: String = originalLogs

        // scrub phone numbers from logs
        originalLogs.extractPhoneNumbers().forEach {
            if (it.length in 10..12 && !it.contains(":") && !it.contains(".")) {
                formattedLogs = formattedLogs.replace(it, "SCRUBBED_PHONE_NUMBER")
            }
        }

        // scrub ipv4 address from logs; preserve local addresses
        originalLogs.extractIpv4Addresses().forEach { formattedLogs = formattedLogs.replace(it, scrubInetAddress(it)) }

        // scrub ipv6 address from logs
        originalLogs.extractIpv6Addresses().forEach { formattedLogs = formattedLogs.replace(it, scrubInetAddress(it)) }


        // Replace sensitive information with placeholders
        return formattedLogs
            .replace(emailPattern, "[SCRUBBED_EMAIL]")
            .replace(macAddressPattern, "[SCRUBBED_MAC_ADDRESS]")
    }


    // https://code.briarproject.org/briar/briar/-/blob/master/bramble-api/src/main/java/org/briarproject/bramble/util/PrivacyUtils.java
    private fun scrubInetAddress(address: String): String {

        try {
            val inetAddress = InetAddress.getByName(address)

            return if (inetAddress is Inet4Address) {
                // Don't scrub local IPv4 addresses
                if (inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress() ||
                    inetAddress.isSiteLocalAddress()) {
                    inetAddress.getHostAddress()
                } else {
                    // Keep first and last octet of non-local IPv4 addresses
                    scrubIpv4Address(inetAddress.getAddress())
                }
            } else {
                // Keep first and last octet of IPv6 addresses
                scrubIpv6Address(inetAddress.address)
            }
        } catch (e: Exception) {
            return address
        }
    }

    private fun scrubIpv4Address(ipv4: ByteArray): String {
        return (ipv4[0].toInt() and 0xFF).toString() + ".[scrubbed]." + (ipv4[3].toInt() and 0xFF)
    }

    private fun scrubIpv6Address(ipv6: ByteArray): String {
        val hex: String = String(toHexChars(ipv6)).lowercase()
        return hex.substring(0, 2) + "[scrubbed]" + hex.substring(30)
    }


    // https://code.briarproject.org/briar/briar/-/blob/master/bramble-api/src/main/java/org/briarproject/bramble/util/StringUtils.java

    /**
     * Converts the given byte array to a hex character array.
     */
    private fun toHexChars(bytes: ByteArray): CharArray {
        val hex = CharArray(bytes.size * 2)
        var i = 0
        var j = 0
        while (i < bytes.size) {
            hex[j++] = HEX[bytes[i].toInt() shr 4 and 0xF]
            hex[j++] = HEX[bytes[i].toInt() and 0xF]
            i++
        }
        return hex
    }

    private val HEX = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    )


}
