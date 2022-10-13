package ie.equalit.ceno.components.ceno

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import ie.equalit.ceno.BrowserActivity

open class OuinetBroadcastReceiver : BroadcastReceiver() {
    // The value constants also force us to use
    // the right type check for the extras bundle.
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: $intent, shutting down Ouinet service")
        val doStop = intent.hasExtra(EXTRA_ACTION_STOP)
        val doPurge = intent.hasExtra(EXTRA_ACTION_PURGE)
        if (!doStop) {
            return  // purging only is not allowed
        }
        stopService(context, doPurge, doClose = true)
    }
    companion object {
        const val EXTRA_ACTION_STOP = "ie.equalit.ceno.OuinetBroadcastReceiver.STOP"
        const val EXTRA_ACTION_PURGE = "ie.equalit.ceno.OuinetBroadcastReceiver.PURGE"
        private const val TAG = "OuinetBroadcastReceiver"

        fun createStopIntent(context: Context): Intent {
            val intent = Intent(context, OuinetBroadcastReceiver::class.java)
            intent.putExtra(EXTRA_ACTION_STOP, 1)
            return intent
        }

        fun createPurgeIntent(context: Context): Intent {
            val intent = createStopIntent(context)
            intent.putExtra(EXTRA_ACTION_PURGE, 1)
            return intent
        }

        fun stopService(context: Context, doPurge : Boolean, doClose : Boolean) {
            OuinetService.stopOuinetService(context)
            killPackageProcess(context, "ouinetService")
            if (doPurge) {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.clearApplicationUserData()
            }
            if (doClose) {
                val closeIntent = Intent(context, BrowserActivity::class.java)
                closeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                closeIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                closeIntent.putExtra(OuinetService.CLOSE_EXTRA, true)
                context.startActivity(closeIntent)
            }
        }

        private fun killPackageProcess(context: Context, name: String) {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
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
                if (process.processName.contains(thisPkg) && process.processName.contains(name)){
                    Log.i(TAG, "Killing process: " + process.processName + " (" + process.pid + ")")
                    Process.killProcess(process.pid)
                }
            }
        }
    }
}