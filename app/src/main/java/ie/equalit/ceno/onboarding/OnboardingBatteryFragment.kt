package ie.equalit.ceno.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import ie.equalit.ceno.BrowserActivity
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentOnboardingBatteryBinding
import ie.equalit.ceno.ext.ceno.onboardingToHome
import ie.equalit.ceno.ext.requireComponents
import mozilla.components.support.base.feature.ActivityResultHandler

/**
 * A simple [Fragment] subclass.
 */
class OnboardingBatteryFragment : Fragment(), ActivityResultHandler {
    private var _binding: FragmentOnboardingBatteryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentOnboardingBatteryBinding.inflate(inflater, container,false)
        container?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ceno_onboarding_background))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            /* This is Android 13 or later, ask for permission POST_NOTIFICATIONS */
            binding.tvOnboardingPermissionText.text = getString(R.string.onboarding_battery_text_v33)
            binding.button.setOnClickListener {
                if (!requireComponents.permissionHandler.isAllowingPostNotifications()) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    findNavController().onboardingToHome()
                }
            }
        }
        else {
            /* This is NOT Android 13, just ask to disable battery optimization */
            binding.tvOnboardingPermissionText.text = getString(R.string.onboarding_battery_text)
            binding.button.setOnClickListener {
                disableBatteryOptimization()
            }
        }
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            requireComponents.ouinet.background.start()
            disableBatteryOptimization()
        }

    private fun disableBatteryOptimization() {
        if (!requireComponents.permissionHandler.requestBatteryOptimizationsOff(requireActivity())) {
            findNavController().onboardingToHome()
        }
    }

    override fun onActivityResult(requestCode: Int, data: Intent?, resultCode: Int): Boolean {
        super.onActivityResult(requestCode, resultCode, data)
        if (requireComponents.permissionHandler.onActivityResult(requestCode, data, resultCode)) {
            findNavController().onboardingToHome()
        } else {
            (activity as BrowserActivity).updateView {
                findNavController().navigate(R.id.action_onboardingBatteryFragment_to_onboardingWarningFragment)
            }
        }
        return true
    }
}