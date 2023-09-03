package ie.equalit.ceno.ext.ceno

import androidx.navigation.NavController
import ie.equalit.ceno.R
import ie.equalit.ceno.settings.CustomPreferenceManager

fun NavController.onboardingToHome() {
    CustomPreferenceManager.setBoolean(context, R.string.pref_key_show_onboarding, false)
    popBackStack(R.id.onboardingFragment, true) // Pop backstack list
    navigate(R.id.action_global_home)

}