package ie.equalit.ceno.components.ceno

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Process
import android.widget.Toast
import ie.equalit.ceno.BuildConfig
import ie.equalit.ceno.R
import ie.equalit.ceno.ext.components
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mozilla.components.concept.fetch.Request
import mozilla.components.support.base.log.logger.Logger

class ClearButtonFeature(
    private val context: Context,
    private val behavior: Int)
{

    private fun clearCenoCache() {
        MainScope().launch {
            var tries = 0
            var success = false
            while (tries < 5 && !success) {
                try {
                    context.components.core.client.fetch(Request(CLEAR_REQUEST_URL)).use {
                        if (it.status == 200) {
                            Logger.debug("Clear cache succeeded try $tries")
                            success = true
                        } else {
                            tries++
                            Logger.debug("Clear cache failed on try $tries")
                            delay(500)
                        }
                    }
                } catch (ex: Exception) {
                    tries++
                    Logger.debug("Clear cache failed on try $tries")
                    delay(500)
                }
            }
            if (tries >= 5 && !success) {
                Logger.debug("Clear cache failed, number of tries exceeded")
                Toast.makeText(context, context.resources.getString(R.string.clear_cache_success), Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(context, context.resources.getString(R.string.clear_cache_success), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /* CENO: Function to create popup opened by purge toolbar button */
    private fun createClearDialog() : AlertDialog {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    Logger.debug("Clear CENO cache and app data selected")
                    /* TODO: Using Toast right before killing the process is bad form, use a different indication */
                    //Toast.makeText(context, "Application data cleared", Toast.LENGTH_SHORT).show()
                    OuinetBroadcastReceiver.stopService(context, doPurge = true, doClose = true)
                }
                DialogInterface.BUTTON_NEUTRAL -> {
                    Logger.debug("Dismissing purge dialog")
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    Logger.debug("Clear CENO cache only selected")
                    clearCenoCache()
                }
            }
        }
        return AlertDialog.Builder(context)
            .setTitle(R.string.ceno_clear_dialog_title)
            .setMessage(R.string.ceno_clear_dialog_description)
            .setPositiveButton(R.string.ceno_clear_dialog_clear_entire_app, dialogClickListener)
            .setNeutralButton(R.string.ceno_clear_dialog_cancel, dialogClickListener)
            .setNegativeButton(R.string.ceno_clear_dialog_clear_cache_only, dialogClickListener)
            .create()
    }

    /* TODO: same code is used by OuinetBroadcastReceiver, should generalize to shared code */
    private fun killPackageProcesses(context: Context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            ?: return
        val processes = am.runningAppProcesses ?: return
        val myPid: Int = Process.myPid()
        val thisPkg = context.packageName
        for (process in processes) {
            if (process.pid == myPid || process.pkgList == null) {
                // Current process will be killed last
                continue
            }
            /* CENO pre-v2 (i.e. java) handled killing the processes like so */
            /*
            val pkgs: MutableList<Array<String>> = Arrays.asList(process.pkgList)
            if (pkgs.contains(arrayOf(thisPkg))) {
                Log.i(TAG, "Killing process: " + process.processName + " (" + process.pid + ")")
                Process.killProcess(process.pid)
            }
            */
            /* Was not able to easily port to kotlin, so using the method below */
            if (process.processName.contains(thisPkg)){
                //Log.i(OuinetBroadcastReceiver.TAG, "Killing process: " + process.processName + " (" + process.pid + ")")
                Process.killProcess(process.pid)
            }
        }
    }

    fun onClick() {
        return when (behavior){
            CLEAR_PROMPT -> createClearDialog().show()
            CLEAR_CACHE -> clearCenoCache()
            CLEAR_APP -> OuinetBroadcastReceiver.stopService(
                context,
                doPurge = true,
                doClose = true
            )
            else -> {}
        }
    }

    companion object {
        const val CLEAR_PROMPT = 0
        const val CLEAR_CACHE = 1
        const val CLEAR_APP = 2
        const val CLEAR_REQUEST_URL = "http://127.0.0.1:"+ BuildConfig.FRONTEND_PORT + "/?purge_cache=do"
    }
}
