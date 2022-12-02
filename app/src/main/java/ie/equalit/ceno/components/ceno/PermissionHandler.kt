package ie.equalit.ceno.components.ceno

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/* CENO: Handles checking which permissions have been granted,
 * adapted from https://pub.dev/packages/flutter_background
 */
class PermissionHandler(private val context: Context) {
    companion object {
        const val PERMISSION_CODE_IGNORE_BATTERY_OPTIMIZATIONS = 5672353
    }

    /*
    fun isWakeLockPermissionGranted(): Boolean
    {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        };
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            // Before Android M, the battery optimization doesn't exist -> Always "ignoring"
            true
        }
    }
    */

    @SuppressLint("BatteryLife")
    fun requestBatteryOptimizationsOff(activity: Activity) : Boolean {
        var result = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Before Android M the battery optimization doesn't exist -> Always "ignoring"
            result = true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            when {
                powerManager.isIgnoringBatteryOptimizations(context.packageName) -> {
                    result = true
                }
                context.checkSelfPermission(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_DENIED -> {
                    result = false
                }
                else -> {
                    val intent = Intent()
                    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    intent.data = Uri.parse("package:${context.packageName}")
                    activity.startActivityForResult(intent, PERMISSION_CODE_IGNORE_BATTERY_OPTIMIZATIONS)
                    result = true
                }
            }
        }
        return result
    }
}
