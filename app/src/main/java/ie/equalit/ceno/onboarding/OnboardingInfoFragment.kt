package ie.equalit.ceno.onboarding

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentOnboardingInfoBinding
import ie.equalit.ceno.ext.requireComponents

/**
 * A simple [Fragment] subclass.
 * Use the [OnboardingInfoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OnboardingInfoFragment : Fragment() {
    private var _binding: FragmentOnboardingInfoBinding? = null
    private val binding get() = _binding!!

    protected val sessionId: String?
        get() = arguments?.getString(SESSION_ID)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentOnboardingInfoBinding.inflate(inflater, container,false);
        container?.background = ContextCompat.getDrawable(requireContext(), R.drawable.onboarding_splash_background)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = view.findNavController()
        binding.button.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (requireComponents.permissionHandler.isAllowingPostNotifications() &&
                    requireComponents.permissionHandler.isIgnoringBatteryOptimizations()
                ) {
                    val action = OnboardingInfoFragmentDirections
                        .actionOnboardingInfoFragmentToOnboardingThanksFragment()
                    navController.navigate(action)
                }
                else {
                    val action = OnboardingInfoFragmentDirections
                        .actionOnboardingInfoFragmentToOnboardingBatteryFragment()
                    navController.navigate(action)
                }
            }
            else {
                if (requireComponents.permissionHandler.isIgnoringBatteryOptimizations()) {
                    val action = OnboardingInfoFragmentDirections
                        .actionOnboardingInfoFragmentToOnboardingThanksFragment()
                    navController.navigate(action)
                }
                else {
                    val action = OnboardingInfoFragmentDirections
                        .actionOnboardingInfoFragmentToOnboardingBatteryFragment()
                    navController.navigate(action)
                }
            }
        }
    }

    companion object {
        private const val SESSION_ID = "session_id"

        @JvmStatic
        protected fun Bundle.putSessionId(sessionId: String?) {
            putString(SESSION_ID, sessionId)
        }

        const val TAG = "ONBOARD_BATTERY"
        fun create(sessionId: String? = null) = OnboardingInfoFragment().apply {
            arguments = Bundle().apply {
                putSessionId(sessionId)
            }
        }
    }
}