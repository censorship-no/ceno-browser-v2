package ie.equalit.ceno.share

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.ShareTabItemBinding
import ie.equalit.ceno.ext.ceno.loadIntoView
import ie.equalit.ceno.ext.components
import mozilla.components.concept.engine.prompt.ShareData

/**
 * Adapter for a list of tabs to be shared.
 */
class ShareTabsAdapter :
    ListAdapter<ShareData, ShareTabsAdapter.ShareTabViewHolder>(ShareTabDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ShareTabViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.share_tab_item, parent, false),
    )

    override fun onBindViewHolder(holder: ShareTabViewHolder, position: Int) =
        holder.bind(getItem(position))

    class ShareTabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: ShareData) = with(itemView) {
            val binding = ShareTabItemBinding.bind(this)
            val url = item.url
            if (!url.isNullOrEmpty()) {
                context.components.core.icons.loadIntoView(binding.shareTabFavicon, url)
            }

            binding.shareTabTitle.text = item.title
            binding.shareTabUrl.text = item.url
        }
    }

    private object ShareTabDiffCallback : DiffUtil.ItemCallback<ShareData>() {
        override fun areItemsTheSame(oldItem: ShareData, newItem: ShareData) =
            oldItem.url == newItem.url

        override fun areContentsTheSame(oldItem: ShareData, newItem: ShareData) =
            oldItem == newItem
    }
}
