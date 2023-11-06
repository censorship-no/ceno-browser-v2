/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings.cleardata

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import ie.equalit.ceno.BrowserActivity
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentClearDataBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClearDataFragment : Fragment(R.layout.fragment_clear_data) {

    private lateinit var controller: DeleteBrowsingDataController
    private var scope: CoroutineScope? = null

    private var _binding: FragmentClearDataBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentClearDataBinding.bind(view)

        getCheckboxes().iterator().forEach {
            it.setOnCheckedChangeListener { _, _ ->
                updateDeleteButton()
            }
        }

        binding.deleteData.setOnClickListener {
            askToDelete()
        }

        updateDeleteButton()
    }

    private fun updateDeleteButton() {
        val enabled = getCheckboxes().any { it.isChecked }

        binding.deleteData.isEnabled = enabled
        binding.deleteData.alpha = if (enabled) ENABLED_ALPHA else DISABLED_ALPHA
    }

    private fun askToDelete() {
        context?.let {
            AlertDialog.Builder(it).apply {
                setMessage(
                    it.getString(
                        R.string.delete_browsing_data_prompt_message_3,
                        it.getString(R.string.app_name),
                    ),
                )

                setNegativeButton(R.string.delete_browsing_data_prompt_cancel) { it: DialogInterface, _ ->
                    it.cancel()
                }

                setPositiveButton(R.string.delete_browsing_data_prompt_allow) { it: DialogInterface, _ ->
                    it.dismiss()
                    deleteSelected()
                }
                create()
            }.show()
        }
    }

    private fun deleteSelected() {
        startDeletion()
        lifecycleScope.launch(IO) {
            getCheckboxes().mapIndexed { _, v ->
                if (v.isChecked) {
                    when (v.text) {
                        getString(R.string.delete_browsing_data) -> controller.deleteBrowsingData()
                        getString(R.string.delete_browser_cache) -> controller.deleteCachedFiles()
                        getString(R.string.delete_all_app_data) -> {
                            (context as BrowserActivity).beginShutdown(true)
                        }
                    }
                }
            }

            withContext(Main) {
                finishDeletion()
            }
        }
    }

    private fun startDeletion() {
        binding.progressBar.visibility = View.VISIBLE
        binding.deleteBrowsingDataWrapper.isEnabled = false
        binding.deleteBrowsingDataWrapper.isClickable = false
        binding.deleteBrowsingDataWrapper.alpha = DISABLED_ALPHA
        Toast.makeText(context, resources.getString(R.string.deleting_browsing_data_in_progress), Toast.LENGTH_SHORT).show()
    }

    private fun finishDeletion() {
        binding.progressBar.visibility = View.GONE
        binding.deleteBrowsingDataWrapper.isEnabled = true
        binding.deleteBrowsingDataWrapper.isClickable = true
        binding.deleteBrowsingDataWrapper.alpha = ENABLED_ALPHA

        Toast.makeText(context, resources.getString(R.string.preferences_delete_browsing_data_snackbar), Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        binding.progressBar.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        scope?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getCheckboxes(): List<AppCompatCheckBox> {
        return listOf(
            binding.browsingData,
            binding.cenoCache,
            binding.allAppData,
        )
    }

    companion object {
        private const val ENABLED_ALPHA = 1f
        private const val DISABLED_ALPHA = 0.6f
    }
}
