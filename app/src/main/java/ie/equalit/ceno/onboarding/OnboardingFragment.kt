package ie.equalit.ceno.onboarding

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import ie.equalit.ceno.R
import ie.equalit.ceno.browser.CenoHomeFragment
import ie.equalit.ceno.databinding.FragmentOnboardingBinding
import ie.equalit.ceno.ext.components
import ie.equalit.ceno.settings.Settings

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    protected val sessionId: String?
        get() = arguments?.getString(SESSION_ID)

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
        binding.button2.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                /* Android 13 or later, always ask for permissions */
                OnboardingBatteryFragment.transitionToFragment(requireActivity(), sessionId)
            }
            else {
                binding.root.background = ContextCompat.getDrawable(requireContext(), R.drawable.onboarding_splash_background)
                transitionToHomeFragment(requireContext(), requireActivity(), sessionId)
            }
        }
    }

    companion object {
        private const val SESSION_ID = "session_id"

        @JvmStatic
        protected fun Bundle.putSessionId(sessionId: String?) {
            putString(SESSION_ID, sessionId)
        }

        const val TAG = "ONBOARD"
        fun create(sessionId: String? = null) = OnboardingFragment().apply {
            arguments = Bundle().apply {
                putSessionId(sessionId)
            }
        }

        fun transitionToHomeFragment(context: Context, activity: FragmentActivity, sessionId: String?) {

            Settings.setShowOnboarding(context , false)

            context.components.useCases.tabsUseCases.addTab(
                CenoHomeFragment.ABOUT_HOME,
                selectTab = true
            )

            activity.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            activity.supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.slide_out,
                    R.anim.slide_back_in,
                    R.anim.fade_out
                )
                replace(
                    R.id.container,
                    CenoHomeFragment.create(sessionId),
                    CenoHomeFragment.TAG
                )
                commit()
            }
        }

    }
}