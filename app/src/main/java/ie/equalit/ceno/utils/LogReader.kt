/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.utils

import android.util.Log

object LogReader {

    fun getLogEntries(): List<String> {
        val logs = mutableListOf<String>()

        try {
            // Change the logcat command to capture specific logs or filters if needed
            val process = Runtime.getRuntime().exec("logcat -d")

            val reader = process.inputStream.bufferedReader()

            var line: String? = reader.readLine()
            while (line != null) {
                logs.add(line)
                line = reader.readLine()
            }
        } catch (e: Exception) {
            Log.e("LogReader", "Error reading logs", e)
        }

        return logs
    }
}
