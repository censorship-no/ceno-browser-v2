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
    private val refreshIntervalMS: Long = 2000
    private val statusTooLong: String = "toolong"

    private var status: Flow<String>? = null

    private var index = 0

    private val displayText: List<Int> = listOf(
        R.string.standby_message_one,
        R.string.standby_message_two,
        R.string.standby_message_three
    )

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

            while (true) {
                val state = requireComponents.ouinet.background.getState()

                if (state != RunningState.Started.toString()) {
                    if (index <= displayText.lastIndex) {
                        emit(getString(displayText[index]))
                        //check for internet
                    } else if (index == 5) {
                        emit(statusTooLong)
                    } else {
                        emit(state)
                    }
                } else {
                    emit(state)
                }
                delay(refreshIntervalMS)
                index += 1
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

        timeoutDialogBuilder.apply {
            setView(timeoutDialogView)
            setPositiveButton("Try Again") { dialogInterface, i ->
                isDialogVisible = false
                tryAgain()
            }
        }

        val dialog = timeoutDialogBuilder.create()
        val btnNetwork = timeoutDialogView.findViewById<Button>(R.id.btn_network_settings)
        btnNetwork?.setOnClickListener {
            isDialogVisible = false
            dialog.dismiss()
            val intent = Intent(ACTION_WIRELESS_SETTINGS)
            startActivity(intent)
        }
        dialog.show()

    }

    private fun tryAgain() {
        //restart progressbar indicator
        binding.progressBar.isActivated = true
        index = 0
    }



}