package ie.equalit.ceno.standby

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_WIRELESS_SETTINGS
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentStandbyBinding
import ie.equalit.ceno.ext.requireComponents
import ie.equalit.ouinet.Ouinet.RunningState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.selectedTab


/**
 * A simple [Fragment] subclass.
 */
class StandbyFragment : Fragment() {

    private var isDialogVisible: Boolean = false
    val refreshIntervalMS: Long = 2500
    val statusTooLong: String = "toolong"

    var status: Flow<String>? = null

    private var _binding : FragmentStandbyBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initiateStatusUpdate()
    }

    private fun isNetworkAvailable(): Boolean {
        val cm : ConnectivityManager = requireContext().getSystemService() ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cap = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networks: Array<Network> = cm.allNetworks
            for (n in networks) {
                val nInfo: NetworkInfo = cm.getNetworkInfo(n) ?: return false
                if (nInfo != null && nInfo.isConnected) return true
            }
        }
        return false
    }

    private fun initiateStatusUpdate() {
        status = flow {
            var index = 0
            while (true) {
                val state = requireComponents.ouinet.background.getState()

                if (state != RunningState.Started.toString()) {
                    if (index <= displayText.lastIndex) {
                        emit(displayText[index])
                        //check for internet
                    } else if (index == displayText.size) {
                        emit(statusTooLong)
                    } else {
                        emit(state)
                    }
                } else {
                    emit(state)
                }
                delay(refreshIntervalMS)
                index += 1
                Log.d("EMIT", state + index)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentStandbyBinding.inflate(inflater, container, false)
        container?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ceno_onboarding_background))

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                status?.collect { currentState ->
                    Log.d("COLLECT", currentState)
                    if (!isDialogVisible) {
                        when(currentState) {
                            RunningState.Started.toString() -> {
                                //go to home or browser
                                findNavController().popBackStack(R.id.standbyFragment, true)
                                if (requireComponents.core.store.state.selectedTab == null)
                                    findNavController().navigate(R.id.action_global_home)
                                else
                                    findNavController().navigate((R.id.action_global_browser))
                            }
                            statusTooLong -> {
                                //Show dialog
                                displayTimeoutDialog()
                            }
                            RunningState.Starting.toString() -> {
                                if (!isNetworkAvailable())
                                    binding.llNoInternet.visibility = View.VISIBLE
                                else
                                    binding.llNoInternet.visibility = View.INVISIBLE
                            }
                            RunningState.Stopped.toString() -> {
                                //restart ouinet? todo

                            }
                            else -> {
                                binding.tvStatus.text = currentState
                                if (!isNetworkAvailable())
                                    binding.llNoInternet.visibility = View.VISIBLE
                                else
                                    binding.llNoInternet.visibility = View.INVISIBLE
                            }
                        }
                    }
                }
            }
        }
    }


    private fun displayTimeoutDialog() {
        isDialogVisible = true

        val timeoutDialogBuilder = AlertDialog.Builder(requireContext())
        val timeoutDialogView = View.inflate(requireContext(), R.layout.layout_standby_timeout, null)

        val btnNetwork = timeoutDialogView.findViewById<Button>(R.id.btn_network_settings)
        btnNetwork?.setOnClickListener {
            isDialogVisible = false
            val intent = Intent(ACTION_WIRELESS_SETTINGS)
            startActivity(intent)
        }

        timeoutDialogBuilder.apply {
            setView(timeoutDialogView)
            setPositiveButton("Try Again") { dialogInterface, i ->
                isDialogVisible = false
                tryAgain()
            }

            show()
        }

    }

    private fun tryAgain() {
        //check for netword and set text

        //restart progressbar indicator
        binding.progressBar.isActivated = true
    }


    companion object {
        val displayText: List<String> = listOf(
            "Connecting to Ceno network",
            "Initializing cache",
            "Finding bootstrap nodes"
        )
    }

}