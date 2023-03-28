package ie.equalit.ceno.browser

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentShutdownBinding
import ie.equalit.ceno.onboarding.OnboardingInfoFragment

/**
 * A simple [Fragment] subclass.
 * Use the [ShutdownFragment.transistionToFragment] factory method to
 * create an instance of this fragment.
 */
class ShutdownFragment : Fragment() {

    var _binding: FragmentShutdownBinding? = null
    val binding get() = _binding!!

    protected val doClear: Boolean?
        get() = arguments?.getBoolean(DO_CLEAR)

    private var fadeInFragmentDuration: Int = 0
    private var timeoutFragmentDuration: Int = 0
    private var isTaskInBack = false
    private val mHandler = Handler(Looper.myLooper()!!)

    private val timeoutCallback = Runnable {
        if (!isTaskInBack) {
            isTaskInBack = requireActivity().moveTaskToBack(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentShutdownBinding.inflate(inflater, container,false);
        fadeInFragmentDuration = resources.getInteger(R.integer.shutdown_fragment_fade_in_duration)
        timeoutFragmentDuration = resources.getInteger(R.integer.shutdown_fragment_timeout_duration)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title.text = if (doClear == true)
            getString(R.string.shutdown_clear_title)
        else
            getString(R.string.shutdown_stop_title)
        mHandler.postDelayed(
            timeoutCallback,
            timeoutFragmentDuration.toLong()
        )
        binding.shutdownLayout.let {
            it.alpha = 0f
            it.visibility = View.VISIBLE
            it.animate()
                .alpha(1f)
                .setDuration(fadeInFragmentDuration.toLong())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacks(timeoutCallback)
    }

    companion object {
        const val TAG = "SHUTDOWN"
        private const val DO_CLEAR = "do_clear"

        @JvmStatic
        protected fun Bundle.putDoClear(doClear: Boolean?) {
            if (doClear != null) {
                putBoolean(DO_CLEAR, doClear)
            }
            else {
                putBoolean(DO_CLEAR, false)
            }
        }

        fun create(doClear: Boolean? = null) = ShutdownFragment().apply {
            arguments = Bundle().apply {
                putDoClear(doClear)
            }
        }

        fun transitionToFragment(activity: FragmentActivity, doClear: Boolean?) {
            activity.supportFragmentManager.beginTransaction().apply {
                add(R.id.container, create(doClear), TAG)
                commit()
            }
        }
    }
}