package ie.equalit.ceno.home

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import ie.equalit.ceno.BrowserActivity
import ie.equalit.ceno.R
import ie.equalit.ceno.browser.BaseBrowserFragment
import ie.equalit.ceno.browser.BrowsingMode
import ie.equalit.ceno.components.ceno.appstate.AppAction
import ie.equalit.ceno.databinding.FragmentHomeBinding
import ie.equalit.ceno.ext.ceno.sort
import ie.equalit.ceno.ext.cenoPreferences
import ie.equalit.ceno.ext.getPreferenceKey
import ie.equalit.ceno.ext.requireComponents
import ie.equalit.ceno.home.sessioncontrol.DefaultSessionControlController
import ie.equalit.ceno.home.sessioncontrol.SessionControlAdapter
import ie.equalit.ceno.home.sessioncontrol.SessionControlInteractor
import ie.equalit.ceno.home.sessioncontrol.SessionControlView
import ie.equalit.ceno.home.topsites.DefaultTopSitesView
import ie.equalit.ceno.utils.CenoPreferences
import ie.equalit.ceno.utils.XMLParser
import mozilla.components.concept.fetch.Request
import ie.equalit.ceno.settings.CenoSettings
import ie.equalit.ceno.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.concept.storage.FrecencyThresholdOption
import mozilla.components.feature.top.sites.TopSitesConfig
import mozilla.components.feature.top.sites.TopSitesFeature
import mozilla.components.feature.top.sites.TopSitesFrecencyConfig
import mozilla.components.feature.top.sites.TopSitesProviderConfig
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import java.util.Locale

/**
 * A [BaseBrowserFragment] subclass that will display the custom CENO Browser homepage
 */
class HomeFragment : BaseHomeFragment() {

    var adapter: SessionControlAdapter? = null

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
        val activity = activity as BrowserActivity
        val components = requireComponents
        themeManager = activity.themeManager
        _binding = FragmentHomeBinding.inflate(inflater, container, false);

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        components.useCases.tabsUseCases.selectTab("")

//        components.appStore.dispatch(AppAction.ModeChange(themeManager.currentMode))

        /* Run coroutine to update the top site store in case it changed since last load */
        scope.launch {
            components.core.cenoTopSitesStorage.getTopSites(components.cenoPreferences.topSitesMaxLimit)
            components.appStore.dispatch(
                AppAction.Change(
                    topSites = components.core.cenoTopSitesStorage.cachedTopSites.sort(),
                    showCenoModeItem = components.cenoPreferences.showCenoModeItem,
                    showThanksCard = components.cenoPreferences.showThanksCard
                )
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
                preferences = components.cenoPreferences,
                appStore = components.appStore,
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

        container?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.blank_background)
        (activity as AppCompatActivity).supportActionBar!!.hide()

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
                maxThreshold = CenoPreferences.TOP_SITES_PROVIDER_MAX_THRESHOLD,
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
//        if (themeManager.currentMode == BrowsingMode.Normal) {
//            sessionControlView?.update(requireComponents.appStore.state)
//        }

        viewLifecycleOwner.lifecycleScope.launch {

            withContext(Dispatchers.Main) {
                if (themeManager.currentMode == BrowsingMode.Normal) {
                    sessionControlView?.update(
                        requireComponents.appStore.state,
                        Settings.getAnnouncementData(requireContext()) /* From local storage */
                    )
                }
                binding.root.consumeFrom(requireComponents.appStore, viewLifecycleOwner) {
                    sessionControlView?.update(
                        it,
                        Settings.getAnnouncementData(requireContext()) /* From local storage */
                    )
                }

                // Switch context to make network call
                withContext(Dispatchers.IO) {

                    // Get language code or fall back to 'en'
                    val languageCode = Locale.getDefault().language.ifEmpty { "en" }

                    var response = CenoSettings.webClientRequest(
                        requireContext(),
                        Request(CenoSettings.getRSSAnnouncementUrl(languageCode))
                    )

                    // if the network call fails, try to load 'en' locale
                    if(response == null) {
                        response = CenoSettings.webClientRequest(
                            requireContext(),
                            Request(CenoSettings.getRSSAnnouncementUrl("en"))
                        )
                    }

                    response?.let { result ->
                        val rssResponse = XMLParser.parseRssXml(result)

                        // perform null-check and save announcement data in local
                        rssResponse?.let { Settings.saveAnnouncementData(requireContext(), it) }

                        // check for null and refresh homepage adapter if necessary
                        if(rssResponse != null) {
                            withContext(Dispatchers.Main) {
                                if (themeManager.currentMode == BrowsingMode.Normal) {
                                    sessionControlView?.update(requireComponents.appStore.state, rssResponse)
                                }

                                binding.root.consumeFrom(requireComponents.appStore, viewLifecycleOwner) {
                                    sessionControlView?.update(it, rssResponse)
                                    updateUI(it.mode)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(mode: BrowsingMode) {
        applyTheme()
        if (mode == BrowsingMode.Personal) {
            binding.homeAppBar.background = ContextCompat.getDrawable(requireContext(), R.color.fx_mobile_private_layer_color_3)
            binding.sessionControlRecyclerView.background = ContextCompat.getDrawable(requireContext(), R.color.fx_mobile_private_layer_color_3)
            binding.wordmark.drawable.setTint(ContextCompat.getColor(requireContext(), R.color.ceno_home_background))
        } else {
            binding.homeAppBar.background = ContextCompat.getDrawable(requireContext(), R.color.ceno_home_background)
            binding.sessionControlRecyclerView.background = ContextCompat.getDrawable(requireContext(), R.color.ceno_home_background)
            binding.wordmark.drawable.setTint(ContextCompat.getColor(requireContext(), R.color.ceno_home_card_public_text))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        updateUI(themeManager.currentMode)
        binding.sessionControlRecyclerView.visibility = View.VISIBLE

        binding.sessionControlRecyclerView.itemAnimator = null
    }
}