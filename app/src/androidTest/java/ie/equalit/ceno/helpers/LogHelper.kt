package ie.equalit.ceno.helpers

import android.util.Log
import ie.equalit.ceno.ext.getSizeInMB
import ie.equalit.ceno.settings.SettingsFragment
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogHelper {

    fun findInLogs( searchString: String, timeWindowInMilliseconds: Long? = null) : Boolean {
        val logEntries = getLogEntries(timeWindowInMilliseconds)
        var result = false
        logEntries.map {
            if (it.contains(searchString))
                result = true
        }
        return result
    }

    private fun getLogEntries(timeWindowInMilliseconds: Long? = null): List<String> {
        val logs = mutableListOf<String>()
        var logsRead = 0

        try {
            // Run logcat command without timestamp filtering if {timeWindowInMilliseconds} is null
            if (timeWindowInMilliseconds == null) {
                return getAllLogs()
            }

            // Regex for identifying timestamp within log
            val timestampRegex = Regex("\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3}")

            // Run logcat command
            val process = ProcessBuilder("logcat", "-d", "*:V").start()

            // Read the output
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null
                && logs.joinToString("\n").getSizeInMB() < SettingsFragment.LOG_FILE_SIZE_LIMIT_MB) {

                // filter out chatty logs as well as logs outside time bound
                if (line?.contains("chatty", ignoreCase = true) == false
                    && isWithinTimeRange(
                        timestampRegex.find(line!!)?.value,
                        timeWindowInMilliseconds
                    )
                ) {
                    logs.add(line!!)
                    logsRead++
                }
            }

            process.waitFor()

        } catch (e: Exception) {
            Log.e("LogReader", "Error reading logs", e)
        }

        return logs
    }

    private fun isWithinTimeRange(timestamp: String?, millisecondDifference: Long): Boolean {

        if (timestamp == null) return false

        val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault())

        // current date-time
        val now = dateFormat.format(Date(System.currentTimeMillis()))

        try {
            val differenceInMillis = (dateFormat.parse(now)?.time
                ?: 0) - (dateFormat.parse(timestamp)?.time ?: 0)

            return differenceInMillis <= millisecondDifference

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun getAllLogs(): List<String> {

        val allLogs = mutableListOf<String>()
        var logsRead = 0

        try {
            // Fetch logs without timestamp filtering
            val process = ProcessBuilder("logcat", "-d", "*:V").start()

            // Read the output
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null
                && allLogs.joinToString("\n").getSizeInMB() < SettingsFragment.LOG_FILE_SIZE_LIMIT_MB) {

                // filter out chatty logs
                if (line?.contains("chatty", ignoreCase = true) == false) {
                    allLogs.add(line!!)
                    logsRead++
                }
            }

            process.waitFor()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return allLogs
    }

}