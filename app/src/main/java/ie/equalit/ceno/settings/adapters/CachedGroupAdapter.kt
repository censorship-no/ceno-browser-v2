/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.settings.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ie.equalit.ceno.databinding.ExpandableListChildItemBinding
import ie.equalit.ceno.databinding.ExpandableListGroupItemBinding


class CachedGroupAdapter(private val groupList: List<GroupItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class GroupViewHolder(private val binding: ExpandableListGroupItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(groupName: String) {
            binding.groupNameTextView.text = groupName
        }
    }

    inner class ChildViewHolder(private val binding: ExpandableListChildItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(childName: String) {
            binding.childNameTextView.text = childName
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_GROUP -> {
                val binding =
                    ExpandableListGroupItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                GroupViewHolder(binding)
            }

            VIEW_TYPE_CHILD -> {
                val binding =
                    ExpandableListChildItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ChildViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_GROUP -> {
                val groupViewHolder = holder as GroupViewHolder
                groupViewHolder.bind(groupList[position].name)
            }

            VIEW_TYPE_CHILD -> {
                val childViewHolder = holder as ChildViewHolder
                val groupPosition = getGroupPosition(position)
                val childPosition = getChildPosition(position)
                val childName = groupList[groupPosition].children[childPosition]
                childViewHolder.bind(childName)
            }
        }
    }

    override fun getItemCount(): Int {
        var count = groupList.size
        groupList.forEach { count += it.children.size }
        return count
    }

    override fun getItemViewType(position: Int): Int {
        return if (isGroupItem(position)) VIEW_TYPE_GROUP else VIEW_TYPE_CHILD
    }

    private fun isGroupItem(position: Int): Boolean {
        var count = 0
        for (group in groupList) {
            if (position == count) {
                return true
            }
            count++
            if (position < count + group.children.size) {
                return false
            }
            count += group.children.size
        }
        return false
    }

    private fun getGroupPosition(position: Int): Int {
        var count = 0
        groupList.forEachIndexed { index, group ->
            if (position == count) {
                return index
            }
            count++
            if (position < count + group.children.size) {
                return index
            }
            count += group.children.size
        }
        return -1
    }

    private fun getChildPosition(position: Int): Int {
        var count = 0
        groupList.forEach { group ->
            if (position < count + group.children.size) {
                return position - count
            }
            count += group.children.size + 1
        }
        return -1
    }

    companion object {
        private const val VIEW_TYPE_GROUP = 0
        private const val VIEW_TYPE_CHILD = 1
    }

    data class GroupItem(val name: String, val children: List<String>)
}