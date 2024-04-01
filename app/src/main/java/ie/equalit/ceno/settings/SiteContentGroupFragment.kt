/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ie.equalit.ceno.settings

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentSiteContentGroupBinding
import ie.equalit.ceno.settings.adapters.CachedGroupAdapter

class SiteContentGroupFragment : Fragment() {

    private var _binding: FragmentSiteContentGroupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSiteContentGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        getActionBar().apply {
            show()
            setTitle(R.string.preferences_ceno_groups_count)
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(requireContext(), R.color.ceno_action_bar)))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("groups")?.let {
            binding.groupListing.setAdapter(
                CachedGroupAdapter(
                    requireContext(),
                    convertToMap(
//                        it.trim()
                        "bbc.co.uk/jahsd\nbbc.co.uk/hsdfn\nbbc.co.uk/ryuw\nfacebook.com\ntwitter.com\ntwitter.com/jfk"
                    )
                )
            )
        }

    }

    private fun convertToMap(groups: String): List<CachedGroupAdapter.GroupItem> {

        val urls = groups.split("\n")

        val map = mutableMapOf<String, MutableList<String>>()

        for (url in urls) {
            val parts = url.split("/")
            val baseUrl = parts.first()
            val subUrls = mutableListOf<String>()
            parts.drop(1).forEach { subUrls.add("$baseUrl/$it") }
            if (subUrls.isEmpty()) subUrls.add(baseUrl)

            map[baseUrl] = if (map[baseUrl].isNullOrEmpty()) subUrls else map[baseUrl].apply { this!!.addAll(subUrls) }!!
        }

        val result = mutableListOf<CachedGroupAdapter.GroupItem>()
        map.keys.forEach { result.add(CachedGroupAdapter.GroupItem(it, map[it]!!.toList())) }

        return result
    }

    private fun getActionBar() = (activity as AppCompatActivity).supportActionBar!!

}