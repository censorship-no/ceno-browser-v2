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
import ie.equalit.ceno.settings.Settings

/**
 * A simple [Fragment] subclass.
 * Use the [OnboardingPublicPvtFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OnboardingPublicPvtFragment : Fragment() {
    private var _binding: FragmentOnboardingPublicPvtBinding? = null
    private val binding get() = _binding!!

    protected val sessionId: String?
        get() = arguments?.getString(SESSION_ID)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
                Settings.setShowOnboarding(requireContext(), false)
                findNavController().popBackStack(R.id.onboardingFragment, true) // Pop backstack list
                findNavController().navigate(R.id.action_global_home)
            }
        }
    }

    companion object {
        private const val SESSION_ID = "session_id"

        @JvmStatic
        protected fun Bundle.putSessionId(sessionId: String?) {
            putString(SESSION_ID, sessionId)
        }

        const val TAG = "ONBOARD_PUBLIC_PVT"
        fun create(sessionId: String? = null) = OnboardingPublicPvtFragment().apply {
            arguments = Bundle().apply {
                putSessionId(sessionId)
            }
        }
    }
}