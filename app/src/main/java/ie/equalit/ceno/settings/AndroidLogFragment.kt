/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings


import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentAndroidLogBinding
import ie.equalit.ceno.ext.EndlessRecyclerViewScrollListener
import ie.equalit.ceno.settings.adapters.LogTextAdapter


class AndroidLogFragment : Fragment() {

    private var _binding: FragmentAndroidLogBinding? = null
    private val binding get() = _binding!!
    private val itemsPerBatch = 40
    private var loadedItemCount = 0
    private lateinit var allItems: List<String>
    private var adapter = LogTextAdapter()

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

        allItems = arguments?.getStringArrayList(SettingsFragment.LOG) ?: emptyList()

        val layoutManager = LinearLayoutManager(requireContext())
        binding.logRecyclerView.layoutManager = layoutManager

        val endlessScrollListener = object : EndlessRecyclerViewScrollListener(layoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                loadMoreData()
            }
        }

        binding.logRecyclerView.addOnScrollListener(endlessScrollListener)
        binding.logRecyclerView.adapter = adapter

        // Load initial data
        loadInitialData()

//        adapter.submitList(allItems)
    }

    private fun loadInitialData() {
        // Load the initial set of data (e.g., the first batch of items)
        val initialData = loadBatchOfItems(0)
        loadedItemCount += initialData.size

        // Notify the adapter about the new data
        adapter.submitList(initialData)
    }

    private fun loadMoreData() {

        Log.d("PPPPPP", "loaded more")
        // Load the next batch of data
        val nextBatch = loadBatchOfItems(loadedItemCount)
        loadedItemCount += nextBatch.size

        // Notify the adapter about the new data
        adapter.submitList(nextBatch)
    }

    private fun loadBatchOfItems(startIndex: Int): List<String> {
        val endIndex = startIndex + itemsPerBatch
        return allItems.subList(startIndex, endIndex.coerceAtMost(allItems.size))
    }

    private fun getActionBar() = (activity as AppCompatActivity).supportActionBar!!

}