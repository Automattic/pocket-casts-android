package au.com.shiftyjelly.pocketcasts.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import au.com.shiftyjelly.pocketcasts.account.databinding.AdapterFrequencyItemBinding
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.settings.util.BillingPeriodHelper
import au.com.shiftyjelly.pocketcasts.utils.extensions.price
import au.com.shiftyjelly.pocketcasts.utils.extensions.shortTitle
import au.com.shiftyjelly.pocketcasts.utils.extensions.trialBillingPeriod
import java.time.Period

class CreateFrequencyAdapter(
    private var list: List<SubscriptionFrequency>,
    private var billingPeriodHelper: BillingPeriodHelper,
    private val clickListener: (SubscriptionFrequency) -> Unit
) : RecyclerView.Adapter<CreateFrequencyAdapter.ViewHolder>() {

    private var selectedSubscriptionFrequency: SubscriptionFrequency? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AdapterFrequencyItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subscriptionFrequency = list[position]
        val selected = (subscriptionFrequency == selectedSubscriptionFrequency)
        holder.bind(subscriptionFrequency, selected)
    }

    override fun getItemCount() = list.size

    fun update(subscriptionFrequency: SubscriptionFrequency?) {
        selectedSubscriptionFrequency = subscriptionFrequency
    }

    fun submitList(newList: List<SubscriptionFrequency>) {
        list = newList
    }

    inner class ViewHolder(val binding: AdapterFrequencyItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            binding.paymentItem.setOnClickListener(this)
        }

        fun bind(subscriptionFrequency: SubscriptionFrequency, selected: Boolean) {
            binding.btnFrequency.isChecked = selected
            binding.txtTitle.text = subscriptionFrequency.product.shortTitle.tryToLocalise(binding.root.resources)

            if (subscriptionFrequency.hint == null) {
                binding.txtDescription.text = null
                binding.txtDescription.visibility = View.GONE
            } else {
                binding.txtDescription.setText(subscriptionFrequency.hint)
                binding.txtDescription.visibility = View.VISIBLE
            }

            if (subscriptionFrequency.product.trialBillingPeriod != null) {
                val trialPeriod = subscriptionFrequency.product.trialBillingPeriod as Period
                val billingDetails = billingPeriodHelper.mapToBillingDetails(trialPeriod)
                binding.txtAmountTop.text = "${billingDetails.periodValue} free"

                // FIXME use Ashita's logic from UpgradeAccountViewModel
                // FIXME string resource
                binding.txtAmountBottom.text = "then ${subscriptionFrequency.product.price} / ${"month"}"

                binding.txtAmountBottom.visibility = View.VISIBLE
            } else {
                binding.txtAmountTop.text = subscriptionFrequency.product.price
                binding.txtAmountBottom.visibility = View.GONE
            }

            binding.outlinePanel.isSelected = selected
        }

        override fun onClick(view: View) {
            if (bindingAdapterPosition == NO_POSITION) {
                return
            }
            val item = list[bindingAdapterPosition]
            selectedSubscriptionFrequency = item
            clickListener(item)
            notifyDataSetChanged()
        }
    }
}
