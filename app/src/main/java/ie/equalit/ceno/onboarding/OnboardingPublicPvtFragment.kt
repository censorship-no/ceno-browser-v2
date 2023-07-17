package ie.equalit.ceno.onboarding

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentOnboardingPublicPvtBinding

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnOnboardingPublicPvt.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.slide_out,
                    R.anim.slide_back_in,
                    R.anim.slide_back_out
                )
                replace(
                    R.id.container,
                    OnboardingInfoFragment.create(sessionId),
                    OnboardingInfoFragment.TAG
                )
                addToBackStack(null)
                commit()
            }
        }
        binding.btnOnboardingStartSkip.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                /* Android 13 or later, always ask for permissions */
                OnboardingBatteryFragment.transitionToFragment(requireActivity(), sessionId)
            }
            else {
                OnboardingFragment.transitionToHomeFragment(requireContext(), requireActivity(), sessionId)
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