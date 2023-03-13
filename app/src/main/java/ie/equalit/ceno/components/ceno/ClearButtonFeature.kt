package ie.equalit.ceno.components.ceno

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import ie.equalit.ceno.R
import ie.equalit.ceno.settings.CenoSettings
import ie.equalit.ceno.settings.OuinetKey
import mozilla.components.support.base.log.logger.Logger

class ClearButtonFeature(
    private val context: Context,
    private val behavior: Int)
{
    /* CENO: Function to create popup opened by purge toolbar button */
    private fun createClearDialog() : AlertDialog {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    Logger.debug("Clear CENO cache and app data selected")
                    /* TODO: Using Toast right before killing the process is bad form, use a different indication */
                    //Toast.makeText(context, "Application data cleared", Toast.LENGTH_SHORT).show()
                    //OuinetBroadcastReceiver.stopService(context, doPurge = true, doClose = true)
                }
                DialogInterface.BUTTON_NEUTRAL -> {
                    Logger.debug("Dismissing purge dialog")
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    Logger.debug("Clear CENO cache only selected")
                    CenoSettings.ouinetClientRequest(context, OuinetKey.PURGE_CACHE)
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

    fun onClick() {
        return when (behavior){
            CLEAR_PROMPT -> createClearDialog().show()
            CLEAR_CACHE -> CenoSettings.ouinetClientRequest(context, OuinetKey.PURGE_CACHE)
            CLEAR_APP -> {}
                /*
                OuinetBroadcastReceiver.stopService(
                context,
                doPurge = true,
                doClose = true
            )
                 */
            else -> {}
        }
    }

    companion object {
        const val CLEAR_PROMPT = 0
        const val CLEAR_CACHE = 1
        const val CLEAR_APP = 2
    }
}
