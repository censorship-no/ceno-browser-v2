package ie.equalit.ceno

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ie.equalit.ceno.databinding.ActivityConsentsBinding
import ie.equalit.ceno.databinding.ConsentViewBinding
import ie.equalit.ceno.databinding.HeaderViewBinding
import org.cleaninsights.sdk.CampaignConsent
import org.cleaninsights.sdk.Consent
import org.cleaninsights.sdk.FeatureConsent

class ConsentsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityConsentsBinding.inflate(layoutInflater)

        val layoutManager = LinearLayoutManager(this)
        val deco = DividerItemDecoration(this, layoutManager.orientation)

        binding.rvConsents.layoutManager = layoutManager
        binding.rvConsents.addItemDecoration(deco)
        binding.rvConsents.adapter = RecyclerViewAdapter()

        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()

        return true
    }

    class HeaderViewHolder(private val binding: HeaderViewBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindView(headingResId: Int) {
            binding.tvHeader.setText(headingResId)
        }
    }

    class ConsentViewHolder(private val binding: ConsentViewBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private var consent: Consent? = null

        init {
            binding.swGrant.setOnClickListener(this)
        }

        fun bindView(consent: FeatureConsent) {
            this.consent = consent

            binding.tvTitle.text = consent.feature.localized(binding.tvTitle.context)
            binding.swGrant.isChecked = consent.granted
            binding.tvStart.setText(R.string.Since_)
            binding.tvStartValue.text = ConsentRequestUi.df.format(consent.startDate)
            binding.tvEnd.visibility = View.GONE
            binding.tvEndValue.visibility = View.GONE
        }

        fun bindView(consent: CampaignConsent) {
            this.consent = consent

            binding.tvTitle.text = consent.campaignId
            binding.swGrant.isChecked = consent.granted
            binding.tvStart.setText(R.string.Start_)
            binding.tvStartValue.text = ConsentRequestUi.df.format(consent.startDate)
            binding.tvEnd.visibility = View.VISIBLE
            binding.tvEndValue.text = ConsentRequestUi.df.format(consent.endDate)
            binding.tvEndValue.visibility = View.VISIBLE
        }

        override fun onClick(view: View?) {
            var featureConsent = consent as? FeatureConsent
            var campaignConsent = consent as? CampaignConsent

            if (featureConsent != null) {
                featureConsent = if (binding.swGrant.isChecked) {
                    BrowserApplication.cleanInsights.grant(featureConsent.feature)
                } else {
                    BrowserApplication.cleanInsights.deny(featureConsent.feature)
                }

                bindView(featureConsent)
            } else if (campaignConsent != null) {
                campaignConsent = if (binding.swGrant.isChecked) {
                    BrowserApplication.cleanInsights.grant(campaignConsent.campaignId)
                } else {
                    BrowserApplication.cleanInsights.deny(campaignConsent.campaignId)
                }

                if (campaignConsent != null) bindView(campaignConsent)
            }
        }
    }

    class RecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            const val TYPE_HEADER = 666
            const val TYPE_CONSENT = 888
        }

        private val featureSize: Int by lazy {
            BrowserApplication.cleanInsights.featureConsentSize
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)

            if (viewType == TYPE_HEADER) {
                return HeaderViewHolder(HeaderViewBinding.inflate(inflater, parent, false))
            }

            return ConsentViewHolder(ConsentViewBinding.inflate(inflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when {
                position == 0 -> {
                    (holder as? HeaderViewHolder)?.bindView(R.string.General_Infos)
                }

                position <= featureSize -> {
                    val consent = BrowserApplication.cleanInsights.getFeatureConsentByIndex(position - 1)
                        ?: return

                    (holder as? ConsentViewHolder)?.bindView(consent)
                }

                position == featureSize + 1 -> {
                    (holder as? HeaderViewHolder)?.bindView(R.string.Campaigns)
                }

                else -> {
                    val consent = BrowserApplication.cleanInsights.getCampaignConsentByIndex(position - featureSize - 2)
                        ?: return

                    (holder as? ConsentViewHolder)?.bindView(consent)
                }
            }
        }

        override fun getItemCount(): Int {
            return 1 + featureSize + 1 + BrowserApplication.cleanInsights.campaignConsentSize
        }

        override fun getItemViewType(position: Int): Int {
            if (position == 0 || position == featureSize + 1) {
                return TYPE_HEADER
            }

            return TYPE_CONSENT
        }
    }
}
