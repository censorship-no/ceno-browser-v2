package ie.equalit.ceno.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentOnboardingThanksBinding
import ie.equalit.ceno.settings.Settings

/**
 * A simple [Fragment] subclass.
 * Use the [OnboardingThanksFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OnboardingThanksFragment : Fragment() {
    private var _binding: FragmentOnboardingThanksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentOnboardingThanksBinding.inflate(inflater, container,false);
        container?.background = ContextCompat.getDrawable(requireContext(), R.drawable.onboarding_splash_background)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.button.setOnClickListener {
            binding.root.background = ContextCompat.getDrawable(requireContext(), R.drawable.onboarding_splash_background)
            Settings.setShowOnboarding(requireContext() , false)
            findNavController().popBackStack(R.id.onboardingFragment, true) // Pop backstack list
            findNavController().navigate(R.id.action_global_home)
        }
    }
}