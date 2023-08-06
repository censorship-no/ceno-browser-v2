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
import ie.equalit.ceno.databinding.FragmentOnboardingPublicPvtBinding
import ie.equalit.ceno.settings.CustomPreferenceManager

/**
 * A simple [Fragment] subclass.
 */
class OnboardingPublicPvtFragment : Fragment() {
    private var _binding: FragmentOnboardingPublicPvtBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentOnboardingPublicPvtBinding.inflate(inflater, container,false)
        container?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ceno_onboarding_background))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnOnboardingPublicPvt.setOnClickListener {
            findNavController().navigate(R.id.action_onboardingPublicPvtFragment_to_onboardingInfoFragment)
        }
        binding.btnOnboardingStartSkip.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                /* Android 13 or later, always ask for permissions */
                findNavController().navigate(R.id.action_onboardingPublicPvtFragment_to_onboardingBatteryFragment)
            }
            else {
                binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ceno_onboarding_background))
                CustomPreferenceManager.setBoolean(requireContext() , R.string.pref_key_show_onboarding, false)
                findNavController().popBackStack(R.id.onboardingFragment, true) // Pop backstack list
                findNavController().navigate(R.id.action_global_home)
            }
        }
    }
}