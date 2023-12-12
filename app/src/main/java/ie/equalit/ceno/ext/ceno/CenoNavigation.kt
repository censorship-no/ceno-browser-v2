package ie.equalit.ceno.ext.ceno

import android.os.Bundle
import android.util.Log
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import ie.equalit.ceno.R
import ie.equalit.ceno.settings.Settings

fun NavController.onboardingToHome() {
    Settings.setShowOnboarding(context, false)
    popBackStack(R.id.onboardingFragment, true) // Pop backstack list
    navigate(R.id.action_global_home)
}

fun NavController.tryNavigate(
    @IdRes resId: Int,
    args: Bundle? = null,
    navOptions: NavOptions? = null
) = try {
    navigate(resId, args, navOptions)
} catch (e: Exception) {
    Log.e("FragmentNavigation", e.message.toString())
}