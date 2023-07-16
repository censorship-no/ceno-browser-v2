package ie.equalit.ceno.onboarding

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentOnboardingBinding
import ie.equalit.ceno.settings.Settings

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container,false)
        container?.background = ContextCompat.getDrawable(requireContext(), R.drawable.onboarding_splash_background)
        (activity as AppCompatActivity).supportActionBar!!.hide()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.button.setOnClickListener {
            findNavController().navigate(R.id.action_onboardingFragment_to_onboardingInfoFragment)
        }
        binding.button2.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                /* Android 13 or later, always ask for permissions */
                findNavController().navigate(R.id.action_onboardingFragment_to_onboardingBatteryFragment)
            }
            else {
                binding.root.background = ContextCompat.getDrawable(requireContext(), R.drawable.onboarding_splash_background)
                Settings.setShowOnboarding(requireContext() , false)
                findNavController().popBackStack(R.id.onboardingFragment, true)
                findNavController().navigate(R.id.action_global_home)
            }
        }
    }
}