package ie.equalit.ceno.settings

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import ie.equalit.ceno.R
import ie.equalit.ceno.ext.getSizeInMB
import ie.equalit.ceno.settings.SettingsFragment.Companion.LOG
import ie.equalit.ceno.settings.SettingsFragment.Companion.LOGS_LAST_10_MINUTES
import ie.equalit.ceno.settings.SettingsFragment.Companion.LOGS_LAST_5_MINUTES
import ie.equalit.ceno.settings.SettingsFragment.Companion.TAG
import ie.equalit.ceno.utils.LogReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ExportAndroidLogsDialog (
    val context: Context,
    val fragment: Fragment
) {
    private val builder: AlertDialog.Builder = AlertDialog.Builder(context)
    private var job: Job? = null

    init {
        val logTimeFilterDialogView = View.inflate(context, R.layout.select_logtime_filter, null)
        val radio5Button = logTimeFilterDialogView.findViewById<RadioButton>(R.id.radio_5_minutes)
        val radio10Button = logTimeFilterDialogView.findViewById<RadioButton>(R.id.radio_10_minutes)

        builder.apply {
            setTitle(R.string.select_log_scope_header)
            setMessage(R.string.select_log_scope_message)
            setView(logTimeFilterDialogView)
            setNegativeButton(R.string.customize_addon_collection_cancel) { dialog: DialogInterface, _ -> dialog.cancel() }
            setPositiveButton(R.string.onboarding_battery_button) { _, _ ->

                // Initialize Android logs

                var logs: MutableList<String>
                var logString: String
                var file: File?

                val progressDialogView = View.inflate(context, R.layout.progress_dialog, null)
                val progressView = progressDialogView.findViewById<ProgressBar>(R.id.progress_bar)

                val progressDialog = AlertDialog.Builder(context)
                    .setView(progressDialogView)
                    .create()
                    .apply {
                        setOnDismissListener {
                            Toast.makeText(context, ContextCompat.getString(context, R.string.canceled), Toast.LENGTH_LONG).show()
                            job?.cancel()
                            dismiss()
                        }
                        progressDialogView.findViewById<ImageButton>(R.id.cancel).setOnClickListener { dismiss() }
                    }

                job = fragment.viewLifecycleOwner.lifecycleScope.launch {

                    withContext(Dispatchers.Main) {

                        progressDialog.show()
                        withContext(Dispatchers.IO) {
                            logs = LogReader.getLogEntries(
                                when {
                                    radio5Button.isChecked -> LOGS_LAST_5_MINUTES
                                    radio10Button.isChecked -> LOGS_LAST_10_MINUTES
                                    else -> null
                                }
                            ) { p ->
                                run {
                                    try {
                                        progressView.progress = p
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }.toMutableList()

                            logString = logs.joinToString("\n")

                            Log.d(TAG, "Log content size: ${logString.getSizeInMB()} MB")

                            // save file to external storage
                            file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.path +"/${ContextCompat.getString(context,
                                R.string.ceno_android_logs_file_name
                            )}.txt")

                            file?.writeText(logString)

                            withContext(Dispatchers.Main) {

                                progressDialog.setOnDismissListener {  } // reset dismissListener

                                progressView.progress = 100
                                delay(200)

                                progressDialog.dismiss()

                                // prompt the user to view or share
                                AlertDialog.Builder(context).apply {
                                    setTitle(context.getString(R.string.ceno_log_file_saved))
                                    setMessage(context.getString(R.string.ceno_log_file_saved_desc))
                                    setNegativeButton(context.getString(R.string.share_logs)) { _, _ ->
                                        if (file?.exists() == true) {

                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                ie.equalit.ceno.BuildConfig.APPLICATION_ID + ".provider",
                                                file!!
                                            )
                                            val intent = Intent(Intent.ACTION_SEND)
                                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            intent.setType("*/*")
                                            intent.putExtra(Intent.EXTRA_STREAM, uri)
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            fragment.startActivity(intent)
                                        }
                                    }
                                    setPositiveButton(context.getString(R.string.view_logs)) { _, _ ->
                                        fragment.findNavController().navigate(
                                            R.id.action_standbyFragment_to_androidLogFragment,
                                            bundleOf().apply {
                                                putStringArrayList(LOG, ArrayList(logs))
                                            }
                                        )
                                    }
                                    create()
                                }.show()
                            }
                        }
                    }
                }
            }
        }
    }

    fun getDialog(): AlertDialog {
        return builder.create()
    }
}