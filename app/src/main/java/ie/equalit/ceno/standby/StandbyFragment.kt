package ie.equalit.ceno.standby

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentStandbyBinding
import ie.equalit.ceno.ext.ceno.onboardingToHome
import ie.equalit.ceno.ext.components
import ie.equalit.ceno.ext.requireComponents
import ie.equalit.ceno.settings.OuinetStatus
import ie.equalit.ouinet.Ouinet.RunningState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.selectedTab

/**
 * A simple [Fragment] subclass.
 * Use the [StandbyFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class StandbyFragment : Fragment() {

    val refreshIntervalMS: Long = 1000

    var status: Flow<String>? = null

    private var _binding : FragmentStandbyBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = flow {
            while (true) {
                val state = requireComponents.ouinet.background.getState()
                emit(state)
                delay(refreshIntervalMS)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentStandbyBinding.inflate(inflater, container, false)
        container?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ceno_onboarding_background))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            status?.collect { currentState ->
                when(currentState) {
                    RunningState.Started.toString() -> {
                        //go to home or browser
                        findNavController().popBackStack()
                        if (requireComponents.core.store.state.selectedTab == null)
                            findNavController().navigate(R.id.action_global_home)
                        else
                            findNavController().navigate((R.id.action_global_browser))
                    }
                    else -> {
                        binding.tvStatus.text = currentState
                    }
                }
            }
        }
    }


}