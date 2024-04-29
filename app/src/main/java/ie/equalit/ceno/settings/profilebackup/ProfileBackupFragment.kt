/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings.profilebackup

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentProfileBackupBinding
import ie.equalit.ceno.ext.requireComponents
import ie.equalit.ceno.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mozilla.components.support.base.log.logger.Logger
import java.io.File

@SuppressWarnings("TooManyFunctions", "LargeClass")
class ProfileBackupFragment : Fragment(R.layout.fragment_profile_backup) {

    private lateinit var controller: ProfileBackupController
    private var scope: CoroutineScope? = null

    private var _binding: FragmentProfileBackupBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProfileBackupBinding.bind(view)
        controller = DefaultProfileBackupController()

        binding.enableSwitch.isChecked = Settings.isOuisyncEnabled(requireContext())

        getCheckboxes().iterator().forEach {
            it.onCheckListener = { _ ->
                updateButtons()
                updatePreference(it)
            }
        }

        getCheckboxes().iterator().forEach {
            it.isChecked = when (it.id) {
                R.id.customization_item -> Settings.backupCustomizations(requireContext())
                R.id.top_site_item -> Settings.backupTopSites(requireContext())
                else -> true
            }
        }

        binding.enableSwitch.setOnClickListener {
            if (Settings.isOuisyncEnabled(requireContext())){
                disableOuisync()
            }
            else {
                enableOuisyncDialog()
            }
        }

        binding.exportProfile.setOnClickListener(getOnClickListenerForExportProfile())
        binding.importProfile.setOnClickListener(getOnClickListenerForImportProfile())
        updateCheckboxes()
        updateButtons()
    }

    private fun updatePreference(it: ProfileBackupItem) {
        when (it.id) {
            R.id.customization_item -> Settings.setBackupCustomizations(requireContext(), it.isChecked)
            R.id.top_site_item -> Settings.setBackupTopSites(requireContext(), it.isChecked)
            else -> return
        }
    }

    override fun onStart() {
        super.onStart()
        /*
        updateTopSitesCount()
        scope = requireComponents.core.store.flowScoped(viewLifecycleOwner) { flow ->
            flow.map { state -> state.tabs.size }
                .distinctUntilChanged()
                .collect { _ -> updateTopSitesCount() }
        }
         */
    }

    override fun onResume() {
        super.onResume()
        //showToolbar(getString(R.string.preferences_delete_browsing_data))

        getCheckboxes().iterator().forEach {
            it.visibility = View.VISIBLE
        }
        //updateItemCounts()
    }

    private fun enableOuisyncDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.enable_ouisync_title)
            setMessage(R.string.enable_ouisync_message)
            setNegativeButton(R.string.customize_addon_collection_cancel) { dialog: DialogInterface, _ -> dialog.cancel() }
            setPositiveButton(R.string.onboarding_battery_button) { _, _ ->
                requireComponents.ouisync.apply {
                    createSession()
                    Settings.setOuisyncEnabled(requireContext(), true)
                    updateCheckboxes()
                    updateButtons()
                    viewLifecycleOwner.lifecycleScope.launch {
                        session.initNetwork(true,true)
                        session.bindNetwork(quicV4 = "0.0.0.0:0", quicV6 = "[::]:0")
                        getProtocolVersion().let {
                            Logger.info("OUISYNC PROTO VERSION: $it")
                        }
                    }
                }
            }
            setOnCancelListener {
                binding.enableSwitch.isChecked = false
            }
            create()
        }.show()
    }

    private fun disableOuisync() {
        Settings.setOuisyncEnabled(requireContext(), false)
        updateCheckboxes()
        updateButtons()
        viewLifecycleOwner.lifecycleScope.launch {
            requireComponents.ouisync.session.close()
        }
    }

    private fun exportPrefsDialog(context : Context) {
        AlertDialog.Builder(context).apply {
            setTitle(R.string.settings_backup_header)
            setMessage(R.string.settings_backup_message)
            setNegativeButton(R.string.customize_addon_collection_cancel) { dialog: DialogInterface, _ -> dialog.cancel() }
            setPositiveButton(R.string.onboarding_battery_button) { _, _ ->
                val backupPrefs: MutableMap<String, *>? =
                    PreferenceManager.getDefaultSharedPreferences(context)?.all
                val prefsIncluded: Array<String> =
                    resources.getStringArray(R.array.prefs_included_in_backup)
                backupPrefs?.iterator()?.let {
                    while (it.hasNext()) {
                        if (!prefsIncluded.contains(it.next().key)) {
                            it.remove()
                        }
                    }
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    requireComponents.ouisync.apply {
                        createAndWriteToRepo("cenoProfile", backupPrefs.toString())
                    }
                }
                /* TODO: intermediate dialog is workaround to wait for writeToken to be set
                 *   instead should use a callback or coroutine */
                AlertDialog.Builder(requireContext()).apply {
                    setTitle("Get your share token!")
                    setPositiveButton(R.string.onboarding_battery_button) { _, _ ->
                        val showText = TextView(requireContext())
                        showText.text = "${requireComponents.ouisync.writeToken}"
                        showText.setTextIsSelectable(true)
                        AlertDialog.Builder(requireContext()).apply {
                            setTitle("Here's your share token!")
                            setView(showText)
                            setPositiveButton(R.string.onboarding_battery_button) { _, _ ->
                            }
                            create()
                        }.show()
                    }
                    create()
                }.show()
            }
            create()
        }.show()
    }

    private fun importPrefsDialog(context : Context) {
        AlertDialog.Builder(context).apply {
            setTitle(R.string.settings_restore_header)
            setMessage(R.string.settings_restore_message)
            setNegativeButton(R.string.customize_addon_collection_cancel) { dialog: DialogInterface, _ -> dialog.cancel() }
            setPositiveButton(R.string.onboarding_battery_button) { _, _ ->
                var content = ""
                viewLifecycleOwner.lifecycleScope.launch {
                    requireComponents.ouisync.apply {
                        content = openAndReadFromRepo("cenoProfile")
                    }
                }
                /* TODO: intermediate dialog is workaround to wait for content to be set
                 *   instead should use a callback or coroutine */
                AlertDialog.Builder(requireContext()).apply {
                    setTitle("Backup found!")
                    setPositiveButton(R.string.onboarding_battery_button) { _, _ ->
                        val showText = TextView(requireContext())
                        showText.text = content
                        showText.setTextIsSelectable(true)
                        AlertDialog.Builder(requireContext()).apply {
                            setTitle("Here's the settings to be restored!")
                            setView(showText)
                            setPositiveButton(R.string.onboarding_battery_button) { _, _ ->
                            }
                            create()
                        }.show()
                    }
                    create()
                }.show()
            }
            create()
        }.show()
    }

    private fun updateCheckboxes() {
        val enabled =  Settings.isOuisyncEnabled(requireContext())

        getCheckboxes().iterator().forEach {
            it.isEnabled = enabled
            it.alpha =  if (enabled) ENABLED_ALPHA else DISABLED_ALPHA
        }
    }


    private fun updateButtons() {
        val enabled =  Settings.isOuisyncEnabled(requireContext()) &&
                getCheckboxes().any { it.isChecked }

        binding.exportProfile.isEnabled = enabled
        binding.exportProfile.alpha = if (enabled) ENABLED_ALPHA else DISABLED_ALPHA

        binding.importProfile.isEnabled = enabled
        binding.importProfile.alpha = if (enabled) ENABLED_ALPHA else DISABLED_ALPHA
    }

    private fun askToExport() {
        context?.let {
            exportPrefsDialog(it)
        }
    }

    private fun askToImport() {
        context?.let {
            importPrefsDialog(it)
        }
    }

    /*
    private fun backupSelected() {
        lifecycleScope.launch(IO) {
            getCheckboxes().mapIndexed { i, v ->
                if (v.isChecked) {
                    when (i) {
                        CUSTOMIZATIONS_INDEX -> controller.getPrefs()
                        TOP_SITES_INDEX -> controller.getTopSites()
                    }
                }
            }

            withContext(Main) {
                finishDeletion()
            }
        }
    }
    */

    private fun startImport() {
        //binding.progressBar.visibility = View.VISIBLE
        binding.profileBackupWrapper.isEnabled = false
        binding.profileBackupWrapper.isClickable = false
        binding.profileBackupWrapper.alpha = DISABLED_ALPHA
        Toast.makeText(context, resources.getString(R.string.deleting_browsing_data_in_progress), Toast.LENGTH_SHORT).show()
    }

    private fun finishImport() {
        val popAfter = binding.customizationItem.isChecked
        //binding.progressBar.visibility = View.GONE
        binding.profileBackupWrapper.isEnabled = true
        binding.profileBackupWrapper.isClickable = true
        binding.profileBackupWrapper.alpha = ENABLED_ALPHA

        //updateItemCounts()

        Toast.makeText(context, resources.getString(R.string.preferences_delete_browsing_data_snackbar), Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        //binding.progressBar.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        scope?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateItemCounts() {
        updateTopSitesCount()
    }

    private fun updateTopSitesCount(openTabs: Int = requireComponents.core.cenoTopSitesStorage.cachedTopSites.size) {
        binding.topSitesItem.apply {
            subtitleView.text = resources.getQuantityString(
                R.plurals.preferences_delete_browsing_data_tabs_subtitle,
                openTabs,
                openTabs
            )
            subtitleView.visibility = View.VISIBLE
        }
    }

    private fun getCheckboxes(): List<ProfileBackupItem> {
        return listOf(
            binding.customizationItem,
            binding.topSitesItem,
        )
    }

    private fun openDirectory(requestCode: Int) {
        // Choose a directory using the system's file picker.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker when it loads.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.getExternalStorageDirectory())
            }
        }
        startActivityForResult(intent, requestCode)
    }


    private fun getOnClickListenerForExportProfile(): View.OnClickListener {
        return View.OnClickListener {
            AlertDialog.Builder(requireContext()).apply {
                setTitle(getString(R.string.export_profile_title))
                setMessage(getString(R.string.export_profile_text))
                setNegativeButton(getString(R.string.ceno_clear_dialog_cancel)) { _, _ ->
                    // show toast for permission denied
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.export_profile_cancelled),
                        Toast.LENGTH_LONG
                    ).show()
                }
                setPositiveButton(getString(R.string.onboarding_battery_button)) { _, _ ->
                    openDirectory(EXPORT_REQUEST_CODE)
                }
                create()
            }.show()
        }
    }

    private fun getOnClickListenerForImportProfile(): View.OnClickListener {
        return View.OnClickListener {
            AlertDialog.Builder(requireContext()).apply {
                setTitle(getString(R.string.import_profile_title))
                setNegativeButton(getString(R.string.ceno_clear_dialog_cancel)) { _, _ ->
                    // show toast for permission denied
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.import_profile_cancelled),
                        Toast.LENGTH_LONG
                    ).show()
                }
                setPositiveButton(getString(R.string.onboarding_battery_button)) { _, _ ->
                    openDirectory(IMPORT_REQUEST_CODE)
                }
                create()
            }.show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK &&
            (requestCode == EXPORT_REQUEST_CODE || requestCode == IMPORT_REQUEST_CODE)) {
            Log.d("ProfileBackupFragment", "Got resultCode: $resultCode and requestCode: $requestCode")
            if (data != null) {
                val uri: Uri? = data.data
                uri?.path?.let { path ->
                    Log.d("ProfileBackupFragment", "uri.path $path")
                    val split: List<String> = path.split(":".toRegex())
                    // TODO: handle other possible storage locations/types
                    val file = File(Environment.getExternalStorageDirectory(), split[1])
                    requireComponents.ouisync.storeDir = file.path
                    if (requestCode == EXPORT_REQUEST_CODE) {
                        Log.d("ProfileBackupFragment", "export to ${file.path}")
                        askToExport()
                    }
                    else {
                        Log.d("ProfileBackupFragment", "import from ${file.path}")
                        askToImport()
                    }
                }
            }
        }
    }

    companion object {
        private const val ENABLED_ALPHA = 1f
        private const val DISABLED_ALPHA = 0.6f

        private const val CUSTOMIZATIONS_INDEX = 0
        private const val TOP_SITES_INDEX = 1
        private const val EXPORT_REQUEST_CODE = 41
        private const val IMPORT_REQUEST_CODE = 42
    }
}
