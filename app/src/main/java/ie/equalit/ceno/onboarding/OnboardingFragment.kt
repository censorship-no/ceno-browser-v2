package ie.equalit.ceno.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ie.equalit.ceno.R
import ie.equalit.ceno.browser.CenoHomeFragment
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
            context?.let { ctx -> Settings.setOnboardingComplete(ctx, true) }
            requireComponents.useCases.tabsUseCases.addTab(CenoHomeFragment.ABOUT_HOME, selectTab = true)
            activity?.supportFragmentManager?.beginTransaction()?.apply {
                replace(
                    R.id.container,
                    CenoHomeFragment.create(sessionId),
                    CenoHomeFragment.TAG
                )
                commit()
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
    }
}