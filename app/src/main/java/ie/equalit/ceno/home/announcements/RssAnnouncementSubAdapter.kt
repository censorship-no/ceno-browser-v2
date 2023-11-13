/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.home.announcements

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ie.equalit.ceno.databinding.RssAnnoucementsSubItemBinding
import ie.equalit.ceno.ext.click
import ie.equalit.ceno.ext.extractATags
import ie.equalit.ceno.ext.getContentFromATag
import ie.equalit.ceno.home.HomepageCardType
import ie.equalit.ceno.home.RssItem
import ie.equalit.ceno.home.sessioncontrol.HomePageInteractor
import ie.equalit.ceno.utils.XMLParser

class RssAnnouncementSubAdapter(private val homePageInteractor: HomePageInteractor) : ListAdapter<RssItem, RssAnnouncementSubAdapter.RssSubItemViewHolder>(RssItemDiffCallback) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RssSubItemViewHolder {
        val binding = RssAnnoucementsSubItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RssSubItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RssSubItemViewHolder, position: Int) {
        holder.bind(getItem(position), homePageInteractor)
    }

    internal object RssItemDiffCallback : DiffUtil.ItemCallback<RssItem>() {
        override fun areItemsTheSame(oldItem: RssItem, newItem: RssItem) = oldItem.pubDate == newItem.pubDate

        override fun areContentsTheSame(oldItem: RssItem, newItem: RssItem) =
            oldItem.description == newItem.description

    }

    class RssSubItemViewHolder(
        val binding: RssAnnoucementsSubItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(rssItem: RssItem, homePageInteractor: HomePageInteractor) {
            binding.itemDate.text = rssItem.pubDate

            var descriptionText = "${rssItem.title} ${rssItem.description}"

            // Replace all a-tags in the description string with a placeholder string
            val allATags = descriptionText.extractATags()
            allATags.forEach { descriptionText = descriptionText.replaceFirst(it, XMLParser.CENO_CUSTOM_PLACEHOLDER) }

            // split the new description string by the placeholder string to generate an array
            val descriptionSubStringArray = descriptionText.split(XMLParser.CENO_CUSTOM_PLACEHOLDER)

            // iteration variable for the list of a-tags
            var index = 0

            // Construct new HTML string to be displayed
            val spannedString = buildSpannedString {
                descriptionSubStringArray.forEach {
                    append(it)
                    val pair = allATags[index].getContentFromATag()
                    click(true, onClick = {
                        homePageInteractor.onUrlClicked(homepageCardType, pair.first.toString())
                    }) {
                        append(pair.second)
                    }
                    index++
                }
            }

            binding.tvMessage.movementMethod = LinkMovementMethod.getInstance()
            binding.tvMessage.text = spannedString
        }
    }

    companion object {
        val homepageCardType = HomepageCardType.ANNOUNCEMENTS_CARD
    }
}
