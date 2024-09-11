package ie.equalit.ceno.settings.dialogs

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LifecycleOwner
import ie.equalit.ceno.R
import ie.equalit.ceno.settings.Settings
import java.util.Locale

class ConsentRequestDialog(val context: Context) {

    fun show(complete: (Boolean) -> Unit) {

        val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()
        } else {
            Locale.getDefault()
        }
        val privacyPolicyUrl = context.getString(R.string.privacy_policy_url)

        val dialogView = View.inflate(context, R.layout.clean_insights_nudge_dialog, null)
        dialogView.findViewById<TextView>(R.id.learn_more).setOnClickListener {
            val dialog = WebViewPopupPanel(context, context as LifecycleOwner, privacyPolicyUrl)
            dialog.show()
        }

        val deviceType = dialogView.findViewById<View>(R.id.checkbox) as CheckBox
        deviceType.isChecked = Settings.isCleanInsightsDeviceTypeIncluded(context)
        deviceType.setOnCheckedChangeListener { _, isChecked ->
            // Save to shared preferences
            Settings.setCleanInsightsDeviceType(context, isChecked)
        }

        val deviceLocale = dialogView.findViewById<View>(R.id.checkbox2) as CheckBox
        deviceLocale.isChecked = Settings.isCleanInsightsDeviceLocaleIncluded(context)
        deviceLocale.setOnCheckedChangeListener { _, isChecked ->
            // Save to shared preferences
            Settings.setCleanInsightsDeviceLocale(context, isChecked)
        }

        AlertDialog.Builder(context)
            .setView(dialogView)
            .setNegativeButton(R.string.clean_insights_maybe_later) { _, _ ->
                complete(false)
            }
            .setPositiveButton(R.string.clean_insights_opt_in) { _, _ ->
                complete(true)
            }
            /* Do nothing when dismissed
            .setOnDismissListener {
            }
            */
            .create()
            .show()
    }
}