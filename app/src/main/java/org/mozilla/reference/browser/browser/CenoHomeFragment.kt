package org.mozilla.reference.browser.browser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import com.google.android.material.appbar.AppBarLayout
import mozilla.components.browser.thumbnails.BrowserThumbnails
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.storage.FrecencyThresholdOption
import mozilla.components.feature.awesomebar.AwesomeBarFeature
import mozilla.components.feature.awesomebar.provider.SearchSuggestionProvider
import mozilla.components.feature.syncedtabs.SyncedTabsStorageSuggestionProvider
import mozilla.components.feature.tabs.toolbar.TabsToolbarFeature
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.feature.top.sites.TopSitesConfig
import mozilla.components.feature.top.sites.TopSitesFeature
import mozilla.components.feature.top.sites.TopSitesProviderConfig
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.reference.browser.BrowserActivity
import org.mozilla.reference.browser.R
import org.mozilla.reference.browser.databinding.FragmentHomeBinding
import org.mozilla.reference.browser.ext.requireComponents
import org.mozilla.reference.browser.ext.cenoPreferences
import org.mozilla.reference.browser.home.sessioncontrol.DefaultSessionControlController
import org.mozilla.reference.browser.home.sessioncontrol.SessionControlAdapter
import org.mozilla.reference.browser.home.sessioncontrol.SessionControlInteractor
import org.mozilla.reference.browser.home.sessioncontrol.SessionControlView
import org.mozilla.reference.browser.home.topsites.DefaultTopSitesView
import org.mozilla.reference.browser.search.AwesomeBarWrapper
import org.mozilla.reference.browser.settings.CenoSupportUtils
import org.mozilla.reference.browser.tabs.TabsTrayFragment
import org.mozilla.reference.browser.utils.CenoPreferences.Companion.TOP_SITES_PROVIDER_MAX_THRESHOLD

/**
 * A [BaseBrowserFragment] subclass that will display the custom CENO Browser homepage
 * Use the [CenoHomeFragment.create] factory method to
 * create an instance of this fragment.
 */
class CenoHomeFragment : BaseBrowserFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    var adapter: SessionControlAdapter? = null

    private var appBarLayout: AppBarLayout? = null

    private val thumbnailsFeature = ViewBoundFeatureWrapper<BrowserThumbnails>()

    private val awesomeBar: AwesomeBarWrapper
        get() = requireView().findViewById(R.id.awesomeBar)
    private val toolbar: BrowserToolbar
        get() = requireView().findViewById(R.id.toolbar)
    private val engineView: EngineView
        get() = requireView().findViewById<View>(R.id.engineView) as EngineView

    private var _sessionControlInteractor: SessionControlInteractor? = null
    private val sessionControlInteractor: SessionControlInteractor
        get() = _sessionControlInteractor!!

    private var sessionControlView: SessionControlView? = null

    private val topSitesFeature = ViewBoundFeatureWrapper<TopSitesFeature>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container,false);
        val activity = activity as BrowserActivity
        val components = requireComponents

        topSitesFeature.set(
            feature = TopSitesFeature(
                view = DefaultTopSitesView(
                    settings = components.cenoPreferences
                ),
                storage = components.core.cenoTopSitesStorage,
                config = ::getTopSitesConfig
            ),
            owner = viewLifecycleOwner,
            view = binding.root
        )

        _sessionControlInteractor = SessionControlInteractor(
            controller = DefaultSessionControlController(
                activity = activity
            )
        )

        /* TODO: make these default sites locale dependent */
        val cenoSite = TopSite.Default(
            id = 1,
            title = requireContext().getString(R.string.default_top_site_ceno),
            url = CenoSupportUtils.CENO_URL,
            createdAt = null
        )

        val wikiSite = TopSite.Default(
            id = 2,
            title = requireContext().getString(R.string.default_top_site_wikipedia),
            url = CenoSupportUtils.WIKIPEDIA_URL,
            createdAt = null
        )

        val apSite = TopSite.Default(
            id = 3,
            title = requireContext().getString(R.string.default_top_site_apnews),
            url = CenoSupportUtils.APNEWS_URL,
            createdAt = null
        )

        val reutersSite = TopSite.Default(
            id = 4,
            title = requireContext().getString(R.string.default_top_site_reuters),
            url = CenoSupportUtils.REUTERS_URL,
            createdAt = null
        )

        val bbcSite = TopSite.Default(
            id = 5,
            title = requireContext().getString(R.string.default_top_site_bbc),
            url = CenoSupportUtils.BBC_URL,
            createdAt = null
        )

        val topSites : List<TopSite> = listOf(
            cenoSite as TopSite,
            wikiSite as TopSite,
            apSite as TopSite,
            reutersSite as TopSite,
            bbcSite as TopSite
        )

        sessionControlView = SessionControlView(
            binding.sessionControlRecyclerView,
            viewLifecycleOwner,
            sessionControlInteractor,
            topSites)

        appBarLayout = binding.homeAppBar
        return binding.root
    }

    /**
     * Returns a [TopSitesConfig] which specifies how many top sites to display and whether or
     * not frequently visited sites should be displayed.
     */
    @VisibleForTesting
    internal fun getTopSitesConfig(): TopSitesConfig {
        val settings = requireContext().cenoPreferences()
        return TopSitesConfig(
            totalSites = settings.topSitesMaxLimit,
            frecencyConfig = FrecencyThresholdOption.SKIP_ONE_TIME_PAGES,
            providerConfig = TopSitesProviderConfig(
                showProviderTopSites = false,//settings.showContileFeature,
                maxThreshold = TOP_SITES_PROVIDER_MAX_THRESHOLD,
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AwesomeBarFeature(awesomeBar, toolbar, engineView)
            .addSearchProvider(
                requireContext(),
                requireComponents.core.store,
                requireComponents.useCases.searchUseCases.defaultSearch,
                fetchClient = requireComponents.core.client,
                mode = SearchSuggestionProvider.Mode.MULTIPLE_SUGGESTIONS,
                engine = requireComponents.core.engine,
                limit = 5,
                filterExactMatch = true
            )
            .addSessionProvider(
                resources,
                requireComponents.core.store,
                requireComponents.useCases.tabsUseCases.selectTab
            )
            .addHistoryProvider(
                requireComponents.core.historyStorage,
                requireComponents.useCases.sessionUseCases.loadUrl
            )
            .addClipboardProvider(requireContext(), requireComponents.useCases.sessionUseCases.loadUrl)

        // We cannot really add a `addSyncedTabsProvider` to `AwesomeBarFeature` coz that would create
        // a dependency on feature-syncedtabs (which depends on Sync).
        awesomeBar.addProviders(
            SyncedTabsStorageSuggestionProvider(
                requireComponents.backgroundServices.syncedTabsStorage,
                requireComponents.useCases.tabsUseCases.addTab,
                requireComponents.core.icons
            )
        )

        TabsToolbarFeature(
            toolbar = toolbar,
            sessionId = sessionId,
            store = requireComponents.core.store,
            showTabs = ::showTabs,
            lifecycleOwner = this
        )

        thumbnailsFeature.set(
            feature = BrowserThumbnails(
                requireContext(),
                engineView,
                requireComponents.core.store
            ),
            owner = this,
            view = view
        )

        /*
         * Remove WebExtension toolbar feature because
         * we don't want the browserAction button in toolbar and
         * the pageAction button created by it didn't work anyway
         */
        /*
        webExtToolbarFeature.set(
            feature = WebExtensionToolbarFeature(
                toolbar,
                requireContext().components.core.store
            ),
            owner = this,
            view = view
        )
        */

        engineView.setDynamicToolbarMaxHeight(resources.getDimensionPixelSize(R.dimen.browser_toolbar_height))
    }

    private fun showTabs() {
        // For now we are performing manual fragment transactions here. Once we can use the new
        // navigation support library we may want to pass navigation graphs around.
        activity?.supportFragmentManager?.beginTransaction()?.apply {
            replace(R.id.container, TabsTrayFragment())
            commit()
        }
    }

    companion object {
        @JvmStatic
        fun create(sessionId: String? = null) = CenoHomeFragment().apply {
            arguments = Bundle().apply {
                putSessionId(sessionId)
            }
        }
    }
}