package ie.equalit.ceno.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import ie.equalit.ceno.BrowserActivity
import ie.equalit.ceno.R
import ie.equalit.ceno.browser.BaseBrowserFragment
import ie.equalit.ceno.components.ceno.appstate.AppAction
import ie.equalit.ceno.databinding.FragmentHomeBinding
import ie.equalit.ceno.ext.ceno.sort
import ie.equalit.ceno.ext.cenoPreferences
import ie.equalit.ceno.ext.requireComponents
import ie.equalit.ceno.home.sessioncontrol.DefaultSessionControlController
import ie.equalit.ceno.home.sessioncontrol.SessionControlAdapter
import ie.equalit.ceno.home.sessioncontrol.SessionControlInteractor
import ie.equalit.ceno.home.sessioncontrol.SessionControlView
import ie.equalit.ceno.home.topsites.DefaultTopSitesView
import ie.equalit.ceno.settings.CustomPreferenceManager
import ie.equalit.ceno.utils.CenoPreferences
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.concept.storage.FrecencyThresholdOption
import mozilla.components.feature.top.sites.TopSitesConfig
import mozilla.components.feature.top.sites.TopSitesFeature
import mozilla.components.feature.top.sites.TopSitesFrecencyConfig
import mozilla.components.feature.top.sites.TopSitesProviderConfig
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper

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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val activity = activity as BrowserActivity
        val components = requireComponents

        components.useCases.tabsUseCases.selectTab("")

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
            topMargin = if(CustomPreferenceManager.getBoolean(requireContext(), R.string.pref_key_toolbar_position)) {
                    resources.getDimensionPixelSize(R.dimen.browser_toolbar_height)
                }
                else {
                    0
                }
        }

        container?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.blank_background)
        (activity as AppCompatActivity).supportActionBar!!.hide()


        if(CustomPreferenceManager.getBoolean(requireContext(), R.string.pref_key_crash_happened, false) &&
            CustomPreferenceManager.getBoolean(requireContext(), R.string.pref_key_show_crash_reporting_permission, true)) {
            // reset the value
            CustomPreferenceManager.setBoolean(requireContext(), R.string.pref_key_crash_happened, false)

            // launch Sentry activation dialog

            val dialogView = View.inflate(requireContext(), R.layout.crash_reporting_nudge_dialog, null)
            val doNotAskAgainCheck = dialogView.findViewById<CheckBox>(R.id.cb_do_not_ask_again)
            val radio1 = dialogView.findViewById<RadioButton>(R.id.radio1)
            val radio2 = dialogView.findViewById<RadioButton>(R.id.radio2)

            val dialog by lazy { AlertDialog.Builder(requireContext()).apply {
                setPositiveButton(getString(R.string.onboarding_warning_button)) { _, _ -> }
            } }

            AlertDialog.Builder(requireContext()).apply {
                setView(dialogView)
                setPositiveButton(getString(R.string.onboarding_battery_button)) { _, _ ->
                    when {
                        doNotAskAgainCheck.isChecked && radio1.isChecked -> {
                            CustomPreferenceManager.setBoolean(requireContext(), R.string.pref_key_allow_crash_reporting, true)
                            dialog.setMessage(getString(R.string.crash_reporting_opt_in))
                            dialog.show()
                        }
                        doNotAskAgainCheck.isChecked && radio2.isChecked -> {
                            CustomPreferenceManager.setBoolean(requireContext(), R.string.pref_key_show_crash_reporting_permission, false)
                            dialog.setMessage(getString(R.string.crash_reporting_opt_out))
                            dialog.show()
                        }
                        !doNotAskAgainCheck.isChecked && radio1.isChecked -> {
                            // TODO: Send once logic
                        }
                        else -> {
                            // Close the dialog
                        }
                    }
                }

                doNotAskAgainCheck.setOnCheckedChangeListener { _, b ->
                    radio1.text = if(b) getString(R.string.crash_reporting_always_send) else getString(R.string.crash_reporting_send_once)
                    radio2.text = if(b) getString(R.string.crash_reporting_never_send) else getString(R.string.crash_reporting_dont_send_this_time)
                }

                create()
            }.show()
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
        sessionControlView?.update(requireComponents.appStore.state)

        binding.root.consumeFrom(requireComponents.appStore, viewLifecycleOwner) {
            sessionControlView?.update(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.privateBrowsingButton.setOnClickListener {
            (activity as BrowserActivity).openToBrowser(
                "about:privatebrowsing",
                newTab = true,
                private = true
            )
        }
        binding.homeAppBar.visibility = View.VISIBLE
        binding.sessionControlRecyclerView.visibility = View.VISIBLE
    }
}