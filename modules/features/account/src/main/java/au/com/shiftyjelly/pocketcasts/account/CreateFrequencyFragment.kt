package au.com.shiftyjelly.pocketcasts.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import au.com.shiftyjelly.pocketcasts.account.databinding.FragmentCreateFrequencyBinding
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountError
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CreateFrequencyFragment : BaseFragment() {

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Inject lateinit var subscriptionManager: SubscriptionManager
    private var adapter: CreateFrequencyAdapter? = null
    private val viewModel: CreateAccountViewModel by activityViewModels()
    private var binding: FragmentCreateFrequencyBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCreateFrequencyBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.paymentsRecyclerView.layoutManager = LinearLayoutManager(context)
        adapter = CreateFrequencyAdapter(emptyList(), theme.activeTheme) {
            subscriptionSelected(it)
        }
        binding.paymentsRecyclerView.adapter = adapter

        viewModel.loadSubs()

        viewModel.createAccountState.observe(viewLifecycleOwner) {
            val progress = binding.progress
            when (it) {
                is CreateAccountState.CurrentlyValid -> {
                    updateForm(false)
                }
                is CreateAccountState.Failure -> {
                    progress.isVisible = false

                    val cannotFindSubs = it.errors.contains(CreateAccountError.CANNOT_LOAD_SUBS)
                    updateForm(cannotFindSubs)
                }
                is CreateAccountState.ProductsLoading -> {
                    progress.isVisible = true
                }
                is CreateAccountState.ProductsLoaded -> {
                    progress.isVisible = false
                    adapter?.submitList(it.list)
                    adapter?.update(viewModel.subscription.value)
                    adapter?.notifyDataSetChanged()
                }
                else -> {}
            }
        }

        binding.btnNext.setOnClickListener {
            val subscription = viewModel.subscription.value
            if (subscription != null) {
                analyticsTracker.track(AnalyticsEvent.SELECT_PAYMENT_FREQUENCY_NEXT_BUTTON_TAPPED, mapOf(PRODUCT_KEY to subscription.productDetails.productId))
                FirebaseAnalyticsTracker.plusPlanChosen(
                    sku = subscription.productDetails.productId,
                    title = subscription.productDetails.title,
                    price = subscription.recurringPricingPhase.pricingPhase.priceAmountMicros * 1_000_000.0,
                    currency = subscription.recurringPricingPhase.pricingPhase.priceCurrencyCode,
                    isFreeTrial = subscription is Subscription.WithTrial,
                )
                it.findNavController().navigate(R.id.action_createFrequencyFragment_to_createTOSFragment)
            }
        }
    }

    private fun subscriptionSelected(subscription: Subscription) {
        viewModel.updateSubscription(subscription)
    }

    private fun updateForm(cannotFindSubs: Boolean) {
        if (cannotFindSubs) {
            UiUtil.displayAlert(
                activity,
                "Server Error",
                "Please check you are logged in to the Google Play Store",
            ) {
                parentFragmentManager.popBackStack()
            }
        }
    }

    companion object {
        private const val PRODUCT_KEY = "product"
    }
}
