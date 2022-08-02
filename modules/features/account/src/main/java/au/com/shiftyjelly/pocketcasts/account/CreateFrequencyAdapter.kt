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
import au.com.shiftyjelly.pocketcasts.utils.extensions.SubscriptionBillingUnit
import au.com.shiftyjelly.pocketcasts.utils.extensions.recurringBillingPeriod
import au.com.shiftyjelly.pocketcasts.utils.extensions.recurringPrice
import au.com.shiftyjelly.pocketcasts.utils.extensions.shortTitle
import au.com.shiftyjelly.pocketcasts.utils.extensions.toSubscriptionBillingUnit
import au.com.shiftyjelly.pocketcasts.utils.extensions.trialBillingPeriod
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.time.Period
import au.com.shiftyjelly.pocketcasts.localization.R as LR

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
                binding.txtAmountTop.text = binding.root.resources.getString(LR.string.profile_amount_free, billingDetails.periodValue ?: "")

                val subscriptionBillingUnit = subscriptionFrequency.product.recurringBillingPeriod?.toSubscriptionBillingUnit()

                when (subscriptionBillingUnit) {
                    SubscriptionBillingUnit.MONTHS -> LR.string.plus_per_month_then
                    SubscriptionBillingUnit.YEARS -> LR.string.plus_per_year_then
                    else -> null
                }.let { stringRes ->
                    if (stringRes == null) {
                        LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "unexpected recurring billing frequency: $subscriptionBillingUnit")
                        binding.txtAmountBottom.visibility = View.GONE
                    } else {
                        val text = binding.root.resources.getString(stringRes, subscriptionFrequency.product.recurringPrice)
                        binding.txtAmountBottom.text = text
                        binding.txtAmountBottom.visibility = View.VISIBLE
                    }
                }
            } else {
                binding.txtAmountTop.text = subscriptionFrequency.product.recurringPrice
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
