package ie.equalit.ceno.onboarding

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ie.equalit.ceno.R
import ie.equalit.ceno.components.ceno.PermissionHandler
import ie.equalit.ceno.databinding.FragmentOnboardingBatteryBinding
import ie.equalit.ceno.ext.requireComponents
import ie.equalit.ceno.settings.Settings
import mozilla.components.support.base.feature.ActivityResultHandler

/**
 * A simple [Fragment] subclass.
 * Use the [OnboardingBatteryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OnboardingBatteryFragment : Fragment(), ActivityResultHandler {
    private var _binding: FragmentOnboardingBatteryBinding? = null
    private val binding get() = _binding!!

    protected val sessionId: String?
        get() = arguments?.getString(SESSION_ID)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentOnboardingBatteryBinding.inflate(inflater, container,false);
        return binding.root
    }

    @SuppressLint("BatteryLife")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.button.setOnClickListener {
            context?.let { ctx ->
                activity?.let { act ->
                    if(!PermissionHandler(ctx).requestBatteryOptimizationsOff(act)) {
                        OnboardingFragment.transitionToHomeFragment(requireComponents, act, sessionId)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, data: Intent?, resultCode: Int): Boolean {
        super.onActivityResult(requestCode, resultCode, data)
        context?.let{ ctx ->
            if (PermissionHandler(ctx).onActivityResult(requestCode, data, resultCode)) {
                Settings.setOnboardingComplete(ctx, true)
                activity?.let { act ->
                    OnboardingFragment.transitionToHomeFragment(requireComponents, act, sessionId)
                }
            }
            else {
                activity?.let { act ->
                    act.supportFragmentManager.beginTransaction().apply {
                        setCustomAnimations(
                            R.anim.slide_in,
                            R.anim.slide_out,
                            R.anim.slide_back_out,
                            R.anim.slide_back_in
                        )
                        replace(
                            R.id.container,
                            OnboardingWarningFragment.create(sessionId),
                            OnboardingWarningFragment.TAG
                        )
                        addToBackStack(null)
                        commit()
                    }
                }
            }
        }
        return true
    }

    companion object {
        private const val SESSION_ID = "session_id"

        @JvmStatic
        protected fun Bundle.putSessionId(sessionId: String?) {
            putString(SESSION_ID, sessionId)
        }

        const val TAG = "ONBOARD_BATTERY"
        fun create(sessionId: String? = null) = OnboardingBatteryFragment().apply {
            arguments = Bundle().apply {
                putSessionId(sessionId)
            }
        }
    }

}