package ie.equalit.ceno.ext.ceno

import androidx.navigation.NavController
import ie.equalit.ceno.R
import ie.equalit.ceno.settings.Settings

fun NavController.onboardingToHome() {
    Settings.setShowOnboarding(context, false)
    popBackStack(R.id.onboardingFragment, true) // Pop backstack list
    navigate(R.id.action_global_home)

}