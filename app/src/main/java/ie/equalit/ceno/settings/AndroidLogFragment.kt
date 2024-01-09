/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings


import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ie.equalit.ceno.R
import ie.equalit.ceno.R.string.empty_log_text
import ie.equalit.ceno.databinding.FragmentAndroidLogBinding


class AndroidLogFragment : Fragment() {

    private var _binding: FragmentAndroidLogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAndroidLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getActionBar().apply {
            show()
            title = getString(R.string.ceno_android_logs_file_name)
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(requireContext(), R.color.ceno_action_bar)))
        }

        arguments?.getString(SettingsFragment.LOG)?.let {
            binding.tvLog.text = SpannableStringBuilder().apply {
                append(it.ifEmpty { getString(R.string.empty_log_text) })
            }
        }
    }

    private fun getActionBar() = (activity as AppCompatActivity).supportActionBar!!
}