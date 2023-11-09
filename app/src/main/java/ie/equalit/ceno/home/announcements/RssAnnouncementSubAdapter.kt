/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.home.announcements

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ie.equalit.ceno.databinding.RssAnnoucementsSubItemBinding
import ie.equalit.ceno.home.RssItem

class RssAnnouncementSubAdapter : ListAdapter<RssItem, RssAnnouncementSubAdapter.RssSubItemViewHolder>(RssItemDiffCallback) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RssSubItemViewHolder {
        val binding = RssAnnoucementsSubItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RssSubItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RssSubItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    internal object RssItemDiffCallback : DiffUtil.ItemCallback<RssItem>() {
        override fun areItemsTheSame(oldItem: RssItem, newItem: RssItem) = oldItem.title == newItem.title

        override fun areContentsTheSame(oldItem: RssItem, newItem: RssItem) =
            oldItem.description == newItem.description

    }

    class RssSubItemViewHolder(
        val binding: RssAnnoucementsSubItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(rssItem: RssItem) {
            binding.itemDate.text = rssItem.pubDate

            "${rssItem.title} ${rssItem.description}".let {
                binding.tvMessage.text = it
            }
        }
    }
}
