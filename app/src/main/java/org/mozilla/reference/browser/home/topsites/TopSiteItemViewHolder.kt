/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser.home.topsites

import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.feature.top.sites.TopSite
import org.mozilla.reference.browser.R
import org.mozilla.reference.browser.databinding.TopSiteItemBinding
import org.mozilla.reference.browser.ext.ceno.bitmapForUrl
import org.mozilla.reference.browser.ext.components
import org.mozilla.reference.browser.ext.ceno.loadIntoView
import org.mozilla.reference.browser.home.sessioncontrol.TopSiteInteractor
import org.mozilla.reference.browser.settings.CenoSupportUtils
import org.mozilla.reference.browser.utils.view.CenoViewHolder

class TopSiteItemViewHolder(
    view: View,
    private val viewLifecycleOwner: LifecycleOwner,
    private val interactor: TopSiteInteractor
) : CenoViewHolder(view) {
    private lateinit var topSite: TopSite
    private val binding = TopSiteItemBinding.bind(view)

    init {
        binding.topSiteItem.setOnLongClickListener {
            interactor.onTopSiteMenuOpened()

            val topSiteMenu = TopSiteItemMenu(
                context = view.context,
                topSite = topSite
            ) { item ->
                when (item) {
                    is TopSiteItemMenu.Item.OpenInPrivateTab -> interactor.onOpenInPrivateTabClicked(
                        topSite
                    )
                    is TopSiteItemMenu.Item.RenameTopSite -> interactor.onRenameTopSiteClicked(
                        topSite
                    )
                    is TopSiteItemMenu.Item.RemoveTopSite -> interactor.onRemoveTopSiteClicked(
                        topSite
                    )
                    is TopSiteItemMenu.Item.Settings -> interactor.onSettingsClicked()
                    is TopSiteItemMenu.Item.SponsorPrivacy -> interactor.onSponsorPrivacyClicked()
                }
            }
            topSiteMenu.menuBuilder.build(view.context).show(anchor = it)
            true
        }
    }

    fun bind(topSite: TopSite, position: Int) {
        binding.topSiteItem.setOnClickListener {
            interactor.onSelectTopSite(topSite, position)
        }

        binding.topSiteTitle.text = topSite.title

        if (topSite is TopSite.Pinned || topSite is TopSite.Default) {
            val pinIndicator = getDrawable(itemView.context, R.drawable.ic_new_pin)
            binding.topSiteTitle.setCompoundDrawablesWithIntrinsicBounds(pinIndicator, null, null, null)
        } else {
            binding.topSiteTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }

        if (topSite is TopSite.Provided) {
            binding.topSiteSubtitle.isVisible = true

            viewLifecycleOwner.lifecycleScope.launch(IO) {
                itemView.context.components.core.client.bitmapForUrl(topSite.imageUrl)?.let { bitmap ->
                    withContext(Main) {
                        binding.faviconImage.setImageBitmap(bitmap)
                    }
                }
            }
        } else {
            /* CENO: Load built-in icons for suggested sites */
            when (topSite.url) {
                CenoSupportUtils.CENO_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_cenomanual))
                }
                CenoSupportUtils.WIKIPEDIA_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_wikipedia))
                }
                CenoSupportUtils.APNEWS_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_apnews))
                }
                CenoSupportUtils.REUTERS_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_reuters))
                }
                CenoSupportUtils.BBC_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_bbc))
                }
                else -> {
                    itemView.context.components.core.icons.loadIntoView(binding.faviconImage, topSite.url)
                }
            }
        }

        this.topSite = topSite
    }

    private fun onTouchEvent(
        v: View,
        event: MotionEvent,
        menu: PopupWindow
    ): Boolean {
        if (event.action == MotionEvent.ACTION_CANCEL) {
            menu.dismiss()
        }
        return v.onTouchEvent(event)
    }

    companion object {
        const val LAYOUT_ID = R.layout.top_site_item
    }
}
