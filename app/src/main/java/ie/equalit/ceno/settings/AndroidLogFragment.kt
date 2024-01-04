package ie.equalit.ceno.settings


import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ie.equalit.ceno.R
import ie.equalit.ceno.databinding.FragmentAndroidLogBinding
import ie.equalit.ceno.ext.share


class AndroidLogFragment : Fragment() {

    private var _binding: FragmentAndroidLogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAndroidLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getActionBar().apply {
            show()
            setTitle("Android Logs")
            set
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(requireContext(), R.color.ceno_action_bar)))
        }

        arguments?.getString("log")?.let {
            binding.tvLog.text = SpannableStringBuilder().apply {
                append(it)
            }

            binding.btnShare.setOnClickListener { _ ->
                requireContext().share(it, "Share")
            }
        }
    }

    private fun getActionBar() = (activity as AppCompatActivity).supportActionBar!!
}