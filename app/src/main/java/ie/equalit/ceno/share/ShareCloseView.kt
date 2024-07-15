package ie.equalit.ceno.share

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import ie.equalit.ceno.databinding.ShareCloseBinding
import mozilla.components.concept.engine.prompt.ShareData

/**
 * Callbacks for possible user interactions on the [ShareCloseView]
 */
interface ShareCloseInteractor {
    fun onShareClosed()
}

class ShareCloseView(
    val containerView: ViewGroup,
    private val interactor: ShareCloseInteractor,
) {

    val adapter = ShareTabsAdapter()

    init {
        val binding = ShareCloseBinding.inflate(
            LayoutInflater.from(containerView.context),
            containerView,
            true,
        )

        binding.closeButton.setOnClickListener { interactor.onShareClosed() }

        binding.sharedSiteList.layoutManager = LinearLayoutManager(containerView.context)
        binding.sharedSiteList.adapter = adapter
    }

    fun setTabs(tabs: List<ShareData>) {
        adapter.submitList(tabs)
    }
}
