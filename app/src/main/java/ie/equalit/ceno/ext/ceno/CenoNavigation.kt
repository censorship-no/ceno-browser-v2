package ie.equalit.ceno.ext.ceno

import androidx.core.os.bundleOf
import androidx.navigation.NavController
import ie.equalit.ceno.Components
import ie.equalit.ceno.R
import ie.equalit.ceno.settings.Settings
import ie.equalit.ceno.standby.StandbyFragment
import ie.equalit.ouinet.Ouinet.RunningState

fun NavController.onboardingToHome(requireComponents: Components) {
    Settings.setShowOnboarding(context, false)
    popBackStack(R.id.onboardingFragment, true) // Pop backstack list

    if (requireComponents.ouinet.background.getState() == RunningState.Started.toString())
        navigate(R.id.action_global_home)
    else {
        val bundle = bundleOf(StandbyFragment.shutdownCeno to false)
        navigate(R.id.action_global_standbyFragment, bundle)
    }

}