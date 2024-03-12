package ie.equalit.ceno.utils

import android.os.Environment


fun isExternalStorageReadOnly(): Boolean {
    val extStorageState = Environment.getExternalStorageState()
    return Environment.MEDIA_MOUNTED_READ_ONLY == extStorageState
}

fun isExternalStorageAvailable(): Boolean {
    val extStorageState = Environment.getExternalStorageState()
    return Environment.MEDIA_MOUNTED == extStorageState
}