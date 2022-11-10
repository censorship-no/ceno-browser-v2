/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.ceno.browser

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.feature.app.links.AppLinksFeature
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.manager.FetchDownloadManager
import mozilla.components.feature.downloads.share.ShareDownloadFeature
import mozilla.components.feature.findinpage.view.FindInPageView
import mozilla.components.feature.prompts.PromptFeature
import mozilla.components.feature.session.FullScreenFeature
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.session.SwipeRefreshFeature
import mozilla.components.feature.sitepermissions.SitePermissionsFeature
import mozilla.components.feature.tabs.WindowFeature
import mozilla.components.feature.webauthn.WebAuthnFeature
import mozilla.components.support.base.feature.ActivityResultHandler
import mozilla.components.support.base.feature.PermissionsFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.view.enterToImmersiveMode
import mozilla.components.support.ktx.android.view.exitImmersiveMode
import ie.equalit.ceno.AppPermissionCodes.REQUEST_CODE_APP_PERMISSIONS
import ie.equalit.ceno.AppPermissionCodes.REQUEST_CODE_DOWNLOAD_PERMISSIONS
import ie.equalit.ceno.AppPermissionCodes.REQUEST_CODE_PROMPT_PERMISSIONS
import ie.equalit.ceno.BuildConfig
import ie.equalit.ceno.R
import ie.equalit.ceno.components.ceno.OuinetBroadcastReceiver
import ie.equalit.ceno.components.ceno.PurgeToolbarAction
import ie.equalit.ceno.databinding.FragmentBrowserBinding
import ie.equalit.ceno.downloads.DownloadService
import ie.equalit.ceno.ext.enableDynamicBehavior
import ie.equalit.ceno.ext.disableDynamicBehavior
import ie.equalit.ceno.ext.getPreferenceKey
import ie.equalit.ceno.ext.requireComponents
import ie.equalit.ceno.pip.PictureInPictureIntegration
import ie.equalit.ceno.tabs.TabsTrayFragment
import mozilla.components.concept.fetch.Request
import java.lang.Exception

/**
 * Base fragment extended by [BrowserFragment] and [ExternalAppBrowserFragment].
 * This class only contains shared code focused on the main browsing content.
 * UI code specific to the app or to custom tabs can be found in the subclasses.
 */
@Suppress("TooManyFunctions")
abstract class BaseBrowserFragment : Fragment(), UserInteractionHandler, ActivityResultHandler {
    var _binding: FragmentBrowserBinding? = null
    val binding get() = _binding!!

    private val sessionFeature = ViewBoundFeatureWrapper<SessionFeature>()
    private val toolbarIntegration = ViewBoundFeatureWrapper<ToolbarIntegration>()
    private val contextMenuIntegration = ViewBoundFeatureWrapper<ContextMenuIntegration>()
    private val downloadsFeature = ViewBoundFeatureWrapper<DownloadsFeature>()
    private val shareDownloadsFeature = ViewBoundFeatureWrapper<ShareDownloadFeature>()
    private val appLinksFeature = ViewBoundFeatureWrapper<AppLinksFeature>()
    private val promptsFeature = ViewBoundFeatureWrapper<PromptFeature>()
    private val fullScreenFeature = ViewBoundFeatureWrapper<FullScreenFeature>()
    private val findInPageIntegration = ViewBoundFeatureWrapper<FindInPageIntegration>()
    private val sitePermissionFeature = ViewBoundFeatureWrapper<SitePermissionsFeature>()
    private val pictureInPictureIntegration = ViewBoundFeatureWrapper<PictureInPictureIntegration>()
    private val swipeRefreshFeature = ViewBoundFeatureWrapper<SwipeRefreshFeature>()
    private val windowFeature = ViewBoundFeatureWrapper<WindowFeature>()
    private val webAuthnFeature = ViewBoundFeatureWrapper<WebAuthnFeature>()

    private val backButtonHandler: List<ViewBoundFeatureWrapper<*>> = listOf(
        fullScreenFeature,
        findInPageIntegration,
        toolbarIntegration,
        sessionFeature
    )

    private val activityResultHandler: List<ViewBoundFeatureWrapper<*>> = listOf(
        webAuthnFeature,
        promptsFeature
    )

    protected val sessionId: String?
        get() = arguments?.getString(SESSION_ID)

    protected var webAppToolbarShouldBeVisible = true

    /* CENO: do not make onCreateView "final", needs to be overridden by CenoHomeFragment */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    /* CENO: not using Jetpack ComposeUI anywhere yet
    *  option was removed from Settings, will be added back if needed */
    //abstract val shouldUseComposeUI: Boolean

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        sessionFeature.set(
            feature = SessionFeature(
                requireComponents.core.store,
                requireComponents.useCases.sessionUseCases.goBack,
                binding.engineView,
                sessionId
            ),
            owner = this,
            view = view
        )

        /* CENO: Add onTabUrlChanged listener to toolbar, to handle fragment transactions */
        toolbarIntegration.set(
            feature = ToolbarIntegration(
                requireContext(),
                requireActivity(),
                binding.toolbar,
                requireComponents.core.historyStorage,
                requireComponents.core.store,
                requireComponents.useCases.sessionUseCases,
                requireComponents.useCases.tabsUseCases,
                requireComponents.useCases.webAppUseCases,
                sessionId,
                ::onTabUrlChanged
            ),
            owner = this,
            view = view
        )

        contextMenuIntegration.set(
            feature = ContextMenuIntegration(
                requireContext(),
                parentFragmentManager,
                requireComponents.core.store,
                requireComponents.useCases.tabsUseCases,
                requireComponents.useCases.contextMenuUseCases,
                binding.engineView,
                view,
                sessionId
            ),
            owner = this,
            view = view
        )

        shareDownloadsFeature.set(
            ShareDownloadFeature(
                context = requireContext().applicationContext,
                httpClient = requireComponents.core.client,
                store = requireComponents.core.store,
                tabId = sessionId
            ),
            owner = this,
            view = view
        )

        downloadsFeature.set(
            feature = DownloadsFeature(
                requireContext(),
                store = requireComponents.core.store,
                useCases = requireComponents.useCases.downloadsUseCases,
                fragmentManager = childFragmentManager,
                downloadManager = FetchDownloadManager(
                    requireContext().applicationContext,
                    requireComponents.core.store,
                    DownloadService::class
                ),
                onNeedToRequestPermissions = { permissions ->
                    // The Fragment class wants us to use registerForActivityResult
                    @Suppress("DEPRECATION")
                    requestPermissions(permissions, REQUEST_CODE_DOWNLOAD_PERMISSIONS)
                }
            ),
            owner = this,
            view = view
        )

        appLinksFeature.set(
            feature = AppLinksFeature(
                requireContext(),
                store = requireComponents.core.store,
                sessionId = sessionId,
                fragmentManager = parentFragmentManager,
                launchInApp = {
                    prefs.getBoolean(requireContext().getPreferenceKey(R.string.pref_key_launch_external_app), false)
                }
            ),
            owner = this,
            view = view
        )

        promptsFeature.set(
            feature = PromptFeature(
                fragment = this,
                store = requireComponents.core.store,
                customTabId = sessionId,
                fragmentManager = parentFragmentManager,
                onNeedToRequestPermissions = { permissions ->
                    // The Fragment class wants us to use registerForActivityResult
                    @Suppress("DEPRECATION")
                    requestPermissions(permissions, REQUEST_CODE_PROMPT_PERMISSIONS)
                }
            ),
            owner = this,
            view = view
        )

        windowFeature.set(
            feature = WindowFeature(requireComponents.core.store, requireComponents.useCases.tabsUseCases),
            owner = this,
            view = view
        )

        fullScreenFeature.set(
            feature = FullScreenFeature(
                store = requireComponents.core.store,
                sessionUseCases = requireComponents.useCases.sessionUseCases,
                tabId = sessionId,
                viewportFitChanged = ::viewportFitChanged,
                fullScreenChanged = ::fullScreenChanged
            ),
            owner = this,
            view = view
        )

        findInPageIntegration.set(
            feature = FindInPageIntegration(
                requireComponents.core.store,
                sessionId,
                binding.findInPageBar as FindInPageView,
                binding.engineView
            ),
            owner = this,
            view = view
        )

        sitePermissionFeature.set(
            feature = SitePermissionsFeature(
                context = requireContext(),
                fragmentManager = parentFragmentManager,
                sessionId = sessionId,
                storage = requireComponents.core.geckoSitePermissionsStorage,
                onNeedToRequestPermissions = { permissions ->
                    // The Fragment class wants us to use registerForActivityResult
                    @Suppress("DEPRECATION")
                    requestPermissions(permissions, REQUEST_CODE_APP_PERMISSIONS)
                },
                onShouldShowRequestPermissionRationale = { shouldShowRequestPermissionRationale(it) },
                store = requireComponents.core.store
            ),
            owner = this,
            view = view
        )

        pictureInPictureIntegration.set(
            feature = PictureInPictureIntegration(
                requireComponents.core.store,
                requireActivity(),
                sessionId
            ),
            owner = this,
            view = view
        )

        swipeRefreshFeature.set(
            feature = SwipeRefreshFeature(
                requireComponents.core.store,
                requireComponents.useCases.sessionUseCases.reload,
                binding.swipeRefresh
            ),
            owner = this,
            view = view
        )

        if (BuildConfig.MOZILLA_OFFICIAL) {
            webAuthnFeature.set(
                feature = WebAuthnFeature(
                    requireComponents.core.engine,
                    requireActivity()
                ),
                owner = this,
                view = view
            )
        }

        /* CENO: Add purge button to toolbar */
        val purgeDialog: AlertDialog = createPurgeDialog()
        binding.toolbar.addBrowserAction(
            PurgeToolbarAction(
                listener = { purgeDialog.show() }
            )
        )

        if (prefs.getBoolean(requireContext().getPreferenceKey(R.string.pref_key_toolbar_hide), false)) {
            binding.toolbar.enableDynamicBehavior(
                requireContext(),
                binding.swipeRefresh,
                binding.engineView,
                prefs.getBoolean(
                    requireContext().getPreferenceKey(R.string.pref_key_toolbar_position),
                    false
                )
            )
        }
        else {
            binding.toolbar.disableDynamicBehavior(
                binding.engineView,
                prefs.getBoolean(
                    requireContext().getPreferenceKey(R.string.pref_key_toolbar_position),
                    false
                )
            )
        }

        /* CENO: not using Jetpack ComposeUI anywhere yet */
        /*
        val composeView = view.findViewById<ComposeView>(R.id.compose_view)
        if (shouldUseComposeUI) {
            composeView.visibility = View.VISIBLE
            composeView.setContent { BrowserToolbar() }

            val params = swipeRefresh.layoutParams as CoordinatorLayout.LayoutParams
            params.topMargin = resources.getDimensionPixelSize(R.dimen.browser_toolbar_height)
            swipeRefresh.layoutParams = params
        }
        */
    }

    override fun onStart() {
        super.onStart()
        /* CENO: Set toolbar appearance based on whether current tab is private or not
         * Doing this in onStart so it does not depend onViewCreated, which isn't run on returning to activity
         */
        requireComponents.core.store.state.selectedTab?.content?.private?.let{ private ->
            binding.toolbar.private = private
            /* TODO: this is messy, should create proper theme manager */
            if (private) {
                binding.toolbar.display.setUrlBackground(
                    context?.let { ctx -> ResourcesCompat.getDrawable(ctx.resources, R.drawable.url_private_background, ctx.theme) }
                )
                binding.toolbar.background = context?.let { ctx -> ResourcesCompat.getDrawable(ctx.resources, R.drawable.toolbar_background, ctx.theme) }
            }
            else {
                binding.toolbar.display.setUrlBackground(
                    context?.let { ctx -> ResourcesCompat.getDrawable(ctx.resources, R.drawable.url_background, ctx.theme) }
                )
                binding.toolbar.background = context?.let { ctx -> ResourcesCompat.getDrawable(ctx.resources, R.drawable.toolbar_dark_background, ctx.theme) }
            }
        }
    }

    /* CENO: Functions to handle hiding/showing the CenoHomeFragment when "about:home" url is requested */
    private fun showHome() {
        activity?.supportFragmentManager?.findFragmentByTag(CenoHomeFragment.TAG)?.let {
            if (it.isVisible) {
                /* CENO: BrowserFragment is already being displayed, don't do another transaction */
                return
            }
        }

        try {
            activity?.supportFragmentManager?.beginTransaction()?.apply {
                replace(R.id.container, CenoHomeFragment.create(sessionId), CenoHomeFragment.TAG)
                commit()
            }
        } catch (ex : Exception) {
            /* Workaround for opening shortcut from homescreen, try again allowing for state loss */
            activity?.supportFragmentManager?.beginTransaction()?.apply {
                replace(R.id.container, CenoHomeFragment.create(sessionId), CenoHomeFragment.TAG)
                commitAllowingStateLoss()
            }
        }
    }

    private fun showBrowser() {
        activity?.supportFragmentManager?.findFragmentByTag(BrowserFragment.TAG)?.let {
            if (it.isVisible) {
                /* CENO: HomeFragment is already being displayed, don't do another transaction */
                return
            }
        }
        try {
            activity?.supportFragmentManager?.beginTransaction()?.apply {
                replace(R.id.container, BrowserFragment.create(sessionId), BrowserFragment.TAG)
                commit()
            }
        }
        catch (ex: Exception){
            /* Workaround for opening shortcut from homescreen, try again allowing for state loss */
            activity?.supportFragmentManager?.beginTransaction()?.apply {
                replace(R.id.container, BrowserFragment.create(sessionId), BrowserFragment.TAG)
                commitAllowingStateLoss()
            }
        }
        /* TODO: Allowing for state loss probably isn't best solution, should figure out how to avoid exception */
    }

    private fun onTabUrlChanged(url : String) {
        activity?.supportFragmentManager?.findFragmentByTag(TabsTrayFragment.TAG)?.let {
            if (it.isVisible) {
                /* CENO: TabsTrayFragment is open, don't switch to home or browser,
                *  TabsTray will handle fragment transactions on it's own */
                return
            }
        }
        if(url == CenoHomeFragment.ABOUT_HOME) {
            showHome()
        }
        else {
            showBrowser()
        }
    }

    /* CENO: Function to create popup opened by purge toolbar button */
    private fun createPurgeDialog() : AlertDialog {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    Logger.debug("Clear CENO cache and app data selected")
                    /* TODO: Using Toast right before killing the process is bad form, use a different indication */
                    //Toast.makeText(context, "Application data cleared", Toast.LENGTH_SHORT).show()
                    OuinetBroadcastReceiver.stopService(requireContext(), doPurge = true, doClose = true)
                }
                DialogInterface.BUTTON_NEUTRAL -> {
                    Logger.debug("Dismissing purge dialog")
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    Logger.debug("Clear CENO cache only selected")
                    requireComponents.core.client.fetch(Request("http://127.0.0.1:8078/?purge_cache=do")).use {
                        if (it.status == 200) {
                            Toast.makeText(context, "Cache purged successfully", Toast.LENGTH_SHORT).show()
                        }
                        else {
                            Toast.makeText(context, "Cache purge failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        /* TODO: Add styling to purge dialog */
        return AlertDialog.Builder(context) //, R.style.PurgeDialog)
            .setTitle(R.string.ceno_purge_dialog_title)
            .setMessage(R.string.ceno_purge_dialog_description)
            .setPositiveButton(R.string.ceno_purge_dialog_purge_entire_app, dialogClickListener)
            .setNeutralButton(R.string.ceno_purge_dialog_cancel, dialogClickListener)
            .setNegativeButton(R.string.ceno_purge_dialog_purge_cache_only, dialogClickListener)
            .create()
    }

    /* TODO: same code is used by OuinetBroadcastReceiver, should generalize to shared code */
    private fun killPackageProcesses(context: Context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            ?: return
        val processes = am.runningAppProcesses ?: return
        val myPid: Int = Process.myPid()
        val thisPkg = context.packageName
        for (process in processes) {
            if (process.pid == myPid || process.pkgList == null) {
                // Current process will be killed last
                continue
            }
            /* CENO pre-v2 (i.e. java) handled killing the processes like so */
            /*
            val pkgs: MutableList<Array<String>> = Arrays.asList(process.pkgList)
            if (pkgs.contains(arrayOf(thisPkg))) {
                Log.i(TAG, "Killing process: " + process.processName + " (" + process.pid + ")")
                Process.killProcess(process.pid)
            }
            */
            /* Was not able to easily port to kotlin, so using the method below */
            if (process.processName.contains(thisPkg)){
                //Log.i(OuinetBroadcastReceiver.TAG, "Killing process: " + process.processName + " (" + process.pid + ")")
                Process.killProcess(process.pid)
            }
        }
    }

    private fun fullScreenChanged(enabled: Boolean) {
        if (enabled) {
            activity?.enterToImmersiveMode()
            binding.toolbar.visibility = View.GONE
            binding.engineView.setDynamicToolbarMaxHeight(0)
        } else {
            activity?.exitImmersiveMode()
            binding.toolbar.visibility = View.VISIBLE
            binding.engineView.setDynamicToolbarMaxHeight(resources.getDimensionPixelSize(R.dimen.browser_toolbar_height))
        }
    }

    private fun viewportFitChanged(viewportFit: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requireActivity().window.attributes.layoutInDisplayCutoutMode = viewportFit
        }
    }

    @CallSuper
    override fun onBackPressed(): Boolean {
        return backButtonHandler.any { it.onBackPressed() }
    }

    final override fun onHomePressed(): Boolean {
        return pictureInPictureIntegration.get()?.onHomePressed() ?: false
    }

    final override fun onPictureInPictureModeChanged(enabled: Boolean) {
        val session = requireComponents.core.store.state.selectedTab
        val fullScreenMode = session?.content?.fullScreen ?: false
        // If we're exiting PIP mode and we're in fullscreen mode, then we should exit fullscreen mode as well.
        if (!enabled && fullScreenMode) {
            onBackPressed()
            fullScreenChanged(false)
        }
    }

    final override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        val feature: PermissionsFeature? = when (requestCode) {
            REQUEST_CODE_DOWNLOAD_PERMISSIONS -> downloadsFeature.get()
            REQUEST_CODE_PROMPT_PERMISSIONS -> promptsFeature.get()
            REQUEST_CODE_APP_PERMISSIONS -> sitePermissionFeature.get()
            else -> null
        }
        feature?.onPermissionsResult(permissions, grantResults)
    }

    companion object {
        private const val SESSION_ID = "session_id"

        @JvmStatic
        protected fun Bundle.putSessionId(sessionId: String?) {
            putString(SESSION_ID, sessionId)
        }
    }

    override fun onActivityResult(requestCode: Int, data: Intent?, resultCode: Int): Boolean {
        Logger.info(
            "Fragment onActivityResult received with " +
                "requestCode: $requestCode, resultCode: $resultCode, data: $data"
        )

        return activityResultHandler.any { it.onActivityResult(requestCode, data, resultCode) }
    }
}
