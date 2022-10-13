/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.home.topsites

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
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.TopSiteItemBinding
import ie.equalit.ceno.ext.ceno.bitmapForUrl
import ie.equalit.ceno.ext.components
import ie.equalit.ceno.ext.ceno.loadIntoView
import ie.equalit.ceno.home.sessioncontrol.TopSiteInteractor
import ie.equalit.ceno.settings.CenoSupportUtils
import ie.equalit.ceno.utils.view.CenoViewHolder

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
                CenoSupportUtils.CENO_URL,
                CenoSupportUtils.CENO_ES_URL,
                CenoSupportUtils.CENO_FA_URL,
                CenoSupportUtils.CENO_MY_URL,
                CenoSupportUtils.CENO_RU_URL,
                CenoSupportUtils.CENO_UK_URL -> {
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
                CenoSupportUtils.BBC_URL,
                CenoSupportUtils.BBC_ES_URL,
                CenoSupportUtils.BBC_FA_URL,
                CenoSupportUtils.BBC_FR_URL,
                CenoSupportUtils.BBC_MY_URL,
                CenoSupportUtils.BBC_RU_URL,
                CenoSupportUtils.BBC_UK_URL,
                CenoSupportUtils.BBC_ZH_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_bbc))
                }
                CenoSupportUtils.ELPAIS_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_elpais))
                }
                CenoSupportUtils.INFOBAE_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_infobae))
                }
                CenoSupportUtils.PASKOOCEH_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_paskoocheh))
                }
                CenoSupportUtils.FACTNAMEH_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_factnameh))
                }
                CenoSupportUtils.COURRIER_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_courrierinternational))
                }
                CenoSupportUtils.LAPRESSE_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_lapresse))
                }
                CenoSupportUtils.MYNOW_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_myanmarnow))
                }
                CenoSupportUtils.JUSTICEMY_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_justiceformyanmar))
                }
                CenoSupportUtils.MEDUZA_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_meduza))
                }
                CenoSupportUtils.MEDIAZONA_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_mediazona))
                }
                CenoSupportUtils.PRAVDA_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_pravda))
                }
                CenoSupportUtils.HROMADSKE_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_hromadske))
                }
                CenoSupportUtils.LTN_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_ltn))
                }
                CenoSupportUtils.TWREPORTER_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.suggestedsites_twreporter))
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
