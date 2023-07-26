package ie.equalit.ceno.onboarding

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentOnboardingInfoBinding
import ie.equalit.ceno.ext.requireComponents
import ie.equalit.ceno.settings.Settings

class OnboardingInfoFragment : Fragment() {
    private var _binding: FragmentOnboardingInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentOnboardingInfoBinding.inflate(inflater, container,false);
        container?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ceno_onboarding_background))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnOnboardingCleanup.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (requireComponents.permissionHandler.isAllowingPostNotifications() &&
                    requireComponents.permissionHandler.isIgnoringBatteryOptimizations()
                ) {
                    findNavController().navigate(R.id.action_onboardingInfoFragment_to_onboardingThanksFragment)
                }
                else {
                    findNavController().navigate(R.id.action_onboardingInfoFragment_to_onboardingBatteryFragment)
                }
            }
            else {
                if (requireComponents.permissionHandler.isIgnoringBatteryOptimizations()) {
                    findNavController().navigate(R.id.action_onboardingInfoFragment_to_onboardingThanksFragment)
                }
                else {
                    findNavController().navigate(R.id.action_onboardingInfoFragment_to_onboardingBatteryFragment)
                }
            }
        }

        binding.btnOnboardingCleanupSkip.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                /* Android 13 or later, always ask for permissions */
                findNavController().navigate(R.id.action_onboardingInfoFragment_to_onboardingBatteryFragment)
            }
            else {
                Settings.setShowOnboarding(requireContext(), false)
                findNavController().popBackStack(R.id.onboardingFragment, true) // Pop backstack list
                findNavController().navigate(R.id.action_global_home)
            }
        }
    }
}