package ie.equalit.ceno.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import ie.equalit.ceno.Components
import ie.equalit.ceno.R
import ie.equalit.ceno.browser.CenoHomeFragment
import ie.equalit.ceno.components.ceno.PermissionHandler
import ie.equalit.ceno.databinding.FragmentOnboardingBinding
import ie.equalit.ceno.ext.requireComponents
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
        _binding = FragmentOnboardingBinding.inflate(inflater, container,false);
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.button.setOnClickListener {
            context?.let { ctx ->
                if (PermissionHandler(ctx).isIgnoringBatteryOptimizations()) {
                    Settings.setOnboardingComplete(ctx, true)
                    activity?.let{ act ->
                        transitionToHomeFragment(requireComponents, act, sessionId)
                    }
                }
                else {
                    activity?.supportFragmentManager?.beginTransaction()?.apply {
                        setCustomAnimations(
                            R.anim.slide_in,
                            R.anim.slide_out,
                            R.anim.slide_back_in,
                            R.anim.slide_back_out
                        )
                        replace(
                            R.id.container,
                            OnboardingBatteryFragment.create(sessionId),
                            OnboardingBatteryFragment.TAG
                        )
                        addToBackStack(null)
                        commit()
                    }
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

        const val TAG = "ONBOARD"
        fun create(sessionId: String? = null) = OnboardingFragment().apply {
            arguments = Bundle().apply {
                putSessionId(sessionId)
            }
        }

        fun transitionToHomeFragment(components: Components, activity: FragmentActivity, sessionId: String?) {
            components.useCases.tabsUseCases.addTab(
                CenoHomeFragment.ABOUT_HOME,
                selectTab = true
            )

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
                addToBackStack(null)
                commit()
            }
        }

    }
}