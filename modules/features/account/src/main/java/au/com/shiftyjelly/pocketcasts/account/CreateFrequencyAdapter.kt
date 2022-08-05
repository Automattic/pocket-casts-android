package au.com.shiftyjelly.pocketcasts.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.LocalContext
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import au.com.shiftyjelly.pocketcasts.account.components.ProductAmountView
import au.com.shiftyjelly.pocketcasts.account.databinding.AdapterFrequencyItemBinding
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class CreateFrequencyAdapter(
    private var list: List<Subscription>,
    private val activeTheme: Theme.ThemeType,
    private val clickListener: (Subscription) -> Unit,
) : RecyclerView.Adapter<CreateFrequencyAdapter.ViewHolder>() {

    private var selectedSubscription: Subscription? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AdapterFrequencyItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subscription = list[position]
        val selected = (subscription == selectedSubscription)
        holder.bind(subscription, selected)
    }

    override fun getItemCount() = list.size

    fun update(subscription: Subscription?) {
        selectedSubscription = subscription
    }

    fun submitList(newList: List<Subscription>) {
        list = newList
    }

    inner class ViewHolder(val binding: AdapterFrequencyItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            binding.paymentItem.setOnClickListener(this)
        }

        fun bind(subscription: Subscription, selected: Boolean) {
            binding.btnFrequency.isChecked = selected
            binding.txtTitle.text = subscription.shortTitle.tryToLocalise(binding.root.resources)

            val hint = subscription.recurringSubscriptionPhase.hint
            if (hint == null) {
                binding.txtDescription.text = null
                binding.txtDescription.visibility = View.GONE
            } else {
                binding.txtDescription.setText(hint)
                binding.txtDescription.visibility = View.VISIBLE
            }

            binding.productAmountView.setContent {
                AppTheme(activeTheme) {
                    when (subscription) {
                        is Subscription.Simple ->
                            ProductAmountView(subscription.recurringSubscriptionPhase.formattedPrice)
                        is Subscription.WithTrial -> {
                            val res = LocalContext.current.resources
                            ProductAmountView(
                                primaryText = subscription.trialSubscriptionPhase.numFree(res),
                                secondaryText = subscription.recurringSubscriptionPhase.thenPriceSlashPeriod(res),
                            )
                        }
                    }
                }
            }
            binding.outlinePanel.isSelected = selected
        }

        override fun onClick(view: View) {
            if (bindingAdapterPosition == NO_POSITION) {
                return
            }
            val item = list[bindingAdapterPosition]
            selectedSubscription = item
            clickListener(item)
            notifyDataSetChanged()
        }
    }
}
