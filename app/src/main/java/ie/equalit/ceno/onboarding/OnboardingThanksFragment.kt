package ie.equalit.ceno.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentOnboardingThanksBinding

/**
 * A simple [Fragment] subclass.
 * Use the [OnboardingThanksFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OnboardingThanksFragment : Fragment() {
    private var _binding: FragmentOnboardingThanksBinding? = null
    private val binding get() = _binding!!

    protected val sessionId: String?
        get() = arguments?.getString(SESSION_ID)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentOnboardingThanksBinding.inflate(inflater, container,false);
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.button.setOnClickListener {
            OnboardingFragment.transitionToHomeFragment(requireContext(), requireActivity(), sessionId)
        }
    }

    companion object {
        private const val SESSION_ID = "session_id"

        @JvmStatic
        protected fun Bundle.putSessionId(sessionId: String?) {
            putString(SESSION_ID, sessionId)
        }

        const val TAG = "ONBOARD_BATTERY"
        fun create(sessionId: String? = null) = OnboardingThanksFragment().apply {
            arguments = Bundle().apply {
                putSessionId(sessionId)
            }
        }

        fun transitionToFragment(activity: FragmentActivity, sessionId: String?) {
            activity.supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.slide_out,
                    R.anim.slide_back_in,
                    R.anim.slide_back_out
                )
                replace(R.id.container, create(sessionId), TAG)
                addToBackStack(null)
                commit()
            }
        }
    }
}