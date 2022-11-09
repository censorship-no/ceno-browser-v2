package ie.equalit.ceno.browser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.browser.thumbnails.BrowserThumbnails
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.storage.FrecencyThresholdOption
import mozilla.components.feature.awesomebar.AwesomeBarFeature
import mozilla.components.feature.awesomebar.provider.SearchSuggestionProvider
import mozilla.components.feature.syncedtabs.SyncedTabsStorageSuggestionProvider
import mozilla.components.feature.tabs.toolbar.TabsToolbarFeature
import mozilla.components.feature.top.sites.TopSitesConfig
import mozilla.components.feature.top.sites.TopSitesFeature
import mozilla.components.feature.top.sites.TopSitesFrecencyConfig
import mozilla.components.feature.top.sites.TopSitesProviderConfig
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import ie.equalit.ceno.BrowserActivity
import ie.equalit.ceno.R
import ie.equalit.ceno.components.ceno.appstate.AppAction
import ie.equalit.ceno.databinding.FragmentBrowserBinding
import ie.equalit.ceno.ext.ceno.sort
import ie.equalit.ceno.ext.requireComponents
import ie.equalit.ceno.ext.cenoPreferences
import ie.equalit.ceno.ext.getPreferenceKey
import ie.equalit.ceno.home.sessioncontrol.DefaultSessionControlController
import ie.equalit.ceno.home.sessioncontrol.SessionControlAdapter
import ie.equalit.ceno.home.sessioncontrol.SessionControlInteractor
import ie.equalit.ceno.home.sessioncontrol.SessionControlView
import ie.equalit.ceno.home.topsites.DefaultTopSitesView
import ie.equalit.ceno.search.AwesomeBarWrapper
import ie.equalit.ceno.tabs.TabsTrayFragment
import ie.equalit.ceno.utils.CenoPreferences.Companion.TOP_SITES_PROVIDER_MAX_THRESHOLD

/**
 * A [BaseBrowserFragment] subclass that will display the custom CENO Browser homepage
 * Use the [CenoHomeFragment.create] factory method to
 * create an instance of this fragment.
 */
class CenoHomeFragment : BaseBrowserFragment() {

    var adapter: SessionControlAdapter? = null

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

    private val scope = MainScope()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentBrowserBinding.inflate(inflater, container,false);
        val activity = activity as BrowserActivity
        val components = requireComponents

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        /* Run coroutine to update the top site store in case it changed since last load */
        scope.launch {
            components.core.cenoTopSitesStorage.getTopSites(components.cenoPreferences.topSitesMaxLimit)
            components.appStore.dispatch(
                AppAction.Change(topSites = components.core.cenoTopSitesStorage.cachedTopSites.sort())
            )
        }

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
                activity = activity,
                viewLifecycleScope = viewLifecycleOwner.lifecycleScope
            )
        )

        sessionControlView = SessionControlView(
            binding.sessionControlRecyclerView,
            viewLifecycleOwner,
            sessionControlInteractor
        )

        updateSessionControlView()

        (binding.homeAppBar.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
            topMargin = if(prefs.getBoolean(requireContext().getPreferenceKey(R.string.pref_key_toolbar_position), false)) {
                    resources.getDimensionPixelSize(R.dimen.browser_toolbar_height)
                }
                else {
                    0
                }
        }

        return binding.root
    }

    /** CENO: Copied from Fenix
     * Returns a [TopSitesConfig] which specifies how many top sites to display and whether or
     * not frequently visited sites should be displayed.
     */
    @VisibleForTesting
    internal fun getTopSitesConfig(): TopSitesConfig {
        val settings = requireContext().cenoPreferences()
        return TopSitesConfig(
            totalSites = settings.topSitesMaxLimit,
            frecencyConfig = TopSitesFrecencyConfig(
                    FrecencyThresholdOption.SKIP_ONE_TIME_PAGES,
            ),
            providerConfig = TopSitesProviderConfig(
                showProviderTopSites = false,//settings.showContileFeature,
                maxThreshold = TOP_SITES_PROVIDER_MAX_THRESHOLD,
            )
        )
    }

    /** CENO: Copied from Fenix
     * The [SessionControlView] is forced to update with our current state when we call
     * [HomeFragment.onCreateView] in order to be able to draw everything at once with the current
     * data in our store. The [View.consumeFrom] coroutine dispatch
     * doesn't get run right away which means that we won't draw on the first layout pass.
     */
    private fun updateSessionControlView() {
        sessionControlView?.update(requireComponents.appStore.state)

        binding.root.consumeFrom(requireComponents.appStore, viewLifecycleOwner) {
            sessionControlView?.update(it)
        }
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
        binding.privateBrowsingButton.setOnClickListener {
            requireComponents.useCases.tabsUseCases.addTab(
                "about:privatebrowsing",
                selectTab = true,
                private = true
            )
        }

        binding.swipeRefresh.visibility = View.GONE
        binding.homeAppBar.visibility = View.VISIBLE
        binding.sessionControlRecyclerView.visibility = View.VISIBLE

        engineView.setDynamicToolbarMaxHeight(resources.getDimensionPixelSize(R.dimen.browser_toolbar_height))
    }

    private fun showTabs() {
        // For now we are performing manual fragment transactions here. Once we can use the new
        // navigation support library we may want to pass navigation graphs around.
        /* CENO: Add this transaction to back stack to go back to correct fragment on back pressed */
        activity?.supportFragmentManager?.beginTransaction()?.apply {
            replace(R.id.container, TabsTrayFragment(), TabsTrayFragment.TAG)
            commit()
        }
    }

    companion object {
        /* CENO: Add default home url and a tag to keep track of whether this fragment is open */
        const val ABOUT_HOME = "about:home"
        const val TAG = "HOME"

        @JvmStatic
        fun create(sessionId: String? = null) = CenoHomeFragment().apply {
            arguments = Bundle().apply {
                putSessionId(sessionId)
            }
        }
    }
}