package ie.equalit.ceno.tooltip

import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import ie.equalit.ceno.R
import ie.equalit.ceno.settings.SettingsFragment
import ie.equalit.ceno.settings.dialogs.LanguageChangeDialog
import java.util.Locale

class CenoTourStartOverlay(
    val fragment: Fragment,
    isPermission: Boolean,
    skipListener:  () -> Unit,
    startListener:  () -> Unit
) {

    private var tooltipOverlay: ConstraintLayout =
        View.inflate(fragment.requireContext(), R.layout.tooltip_start_overlay_layout, null) as ConstraintLayout
    private var containerView: FrameLayout? = fragment.activity?.findViewById(R.id.container)
    private var btnStartTour: Button
    private var btnSkipTour: Button

    init {
        tooltipOverlay.setBackgroundColor(ContextCompat.getColor(fragment.requireContext(), R.color.tooltip_background))
        val localeLayout = tooltipOverlay.findViewById<LinearLayout>(R.id.locale_picker_layout)
        btnSkipTour = tooltipOverlay.findViewById(R.id.btn_skip_all_ceno_tour)
        btnStartTour = tooltipOverlay.findViewById(R.id.btn_start_ceno_tour)
        btnSkipTour.setOnClickListener {
            remove()
            skipListener.invoke()
        }
        btnStartTour.setOnClickListener {
            remove()
            startListener.invoke()
        }

        if (isPermission) {
            //change text
            val title = tooltipOverlay.findViewById<TextView>(R.id.tv_start_tooltip_title)
            val description = tooltipOverlay.findViewById<TextView>(R.id.tv_start_tooltip_description)
            title.text = fragment.getString(R.string.onboarding_permissions_title)
            description.text = fragment.getString(R.string.tooltip_permission_text)
            btnStartTour.text = fragment.getString(R.string.onboarding_battery_button)
            //hide skip button
            btnSkipTour.visibility = View.GONE
            localeLayout.visibility = View.GONE
        } else {
            val localePicker = tooltipOverlay.findViewById<TextView>(R.id.locale_picker)
            localePicker.text = SettingsFragment.getCurrentLocale().displayLanguage
            localePicker.setOnClickListener {
                val languageChangeDialog = LanguageChangeDialog(
                    fragment.requireContext(),
                    object : LanguageChangeDialog.SetLanguageListener {
                        override fun onLanguageSelected(locale: Locale) {
                            // update language
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.create(Locale.forLanguageTag(locale.toLanguageTag()))
                            )
                            ie.equalit.ceno.settings.Settings.clearAnnouncementData(fragment.requireContext())
                        }

                    }
                )
                languageChangeDialog.getDialog().show()
                true
            }
        }

        tooltipOverlay.isClickable = true
    }

    fun show() {
        containerView?.addView(tooltipOverlay)
    }

    fun remove() {
        containerView?.removeView(tooltipOverlay)
    }
}