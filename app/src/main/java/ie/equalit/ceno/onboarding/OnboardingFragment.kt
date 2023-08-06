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
import ie.equalit.ceno.settings.CustomPreferenceManager

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container,false)
        container?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ceno_onboarding_background))
        (activity as AppCompatActivity).supportActionBar!!.hide()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnOnboardingStart.setOnClickListener {
            findNavController().navigate(R.id.action_onboardingFragment_to_onboardingPublicPvtFragment)
        }

        binding.btnOnboardingStartSkip.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                /* Android 13 or later, always ask for permissions */
                findNavController().navigate(R.id.action_onboardingFragment_to_onboardingBatteryFragment)
            } else {
                binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ceno_onboarding_background))
                CustomPreferenceManager.setBoolean(requireContext() , R.string.pref_key_show_onboarding, false)
                findNavController().popBackStack(R.id.onboardingFragment, true) // Pop backstack list
                findNavController().navigate(R.id.action_global_home)
            }
        }
    }
}