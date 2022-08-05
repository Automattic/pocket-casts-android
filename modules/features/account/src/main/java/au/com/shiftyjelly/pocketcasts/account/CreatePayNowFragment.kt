package au.com.shiftyjelly.pocketcasts.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import au.com.shiftyjelly.pocketcasts.account.components.ProductAmountView
import au.com.shiftyjelly.pocketcasts.account.databinding.FragmentCreatePaynowBinding
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountError
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getTintedDrawable
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class CreatePayNowFragment : BaseFragment() {

    @Inject lateinit var subscriptionManager: SubscriptionManager

    private val viewModel: CreateAccountViewModel by activityViewModels()
    private var binding: FragmentCreatePaynowBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCreatePaynowBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = (activity as AppCompatActivity).findViewById<Toolbar>(R.id.toolbar)
        context?.let {
            val closeColor = it.getThemeColor(UR.attr.secondary_icon_01)
            toolbar.navigationIcon = it.getTintedDrawable(IR.drawable.ic_close, closeColor)
        }
        toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding?.profileCircleView?.setup(1.0f, true)

        viewModel.subscription.value?.let { subscription ->
            binding?.txtCharge?.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AppTheme(theme.activeTheme) {

                        val res = LocalContext.current.resources
                        when (subscription) {
                            is Subscription.Simple -> ProductAmountView(
                                primaryText = subscription.recurringPricingPhase.priceSlashPeriod(res),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            )
                            is Subscription.WithTrial -> ProductAmountView(
                                primaryText = subscription.trialPricingPhase.numFree(res),
                                secondaryText = subscription.recurringPricingPhase.thenPriceSlashPeriod(res),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            )
                        }
                    }
                }
            }
            binding?.txtRenews?.setText(subscription.recurringPricingPhase.renews)
            binding?.txtEmail?.text = viewModel.email.value
            binding?.txtSubscription?.text = getString(LR.string.pocket_casts_plus)
            displayMainLayout(show = true, subscription = subscription)

            viewModel.createAccountState.observe(
                viewLifecycleOwner,
                Observer {
                    val binding = binding ?: return@Observer
                    val progress = binding.progress
                    val txtError = binding.txtError
                    when (it) {
                        is CreateAccountState.AccountCreated -> {
                            progress.isVisible = false
                            txtError.text = ""
                        }
                        is CreateAccountState.SubscriptionCreating -> {
                            progress.isVisible = true
                            txtError.text = ""
                        }
                        is CreateAccountState.SubscriptionCreated -> {
                            progress.isVisible = false
                            txtError.text = ""
                            if (view.findNavController().currentDestination?.id == R.id.createPayNowFragment) {
                                view.findNavController()
                                    .navigate(R.id.action_createPayNowFragment_to_createDoneFragment)
                            }
                        }
                        is CreateAccountState.Failure -> {
                            progress.isVisible = false
                            // val cancelled = it.errors.contains(CreateAccountError.CANCELLED_CREATE_SUB)
                            val serverFail =
                                it.errors.contains(CreateAccountError.CANNOT_CREATE_SUB)
                            if (serverFail) {
                                displayMainLayout(false, subscription = subscription)
                                txtError.text =
                                    getString(LR.string.profile_create_subscription_failed)
                                viewModel.clearError(CreateAccountError.CANNOT_CREATE_SUB)
                            }
                        }
                        else -> {}
                    }
                }
            )

            binding?.btnSubmit?.setOnClickListener {
                if (viewModel.createAccountState.value == CreateAccountState.AccountCreated ||
                    viewModel.createAccountState.value == CreateAccountState.CurrentlyValid
                ) {

                    binding?.txtError?.text = ""
                    displayMainLayout(true, subscription = subscription)
                    viewModel.subscription.value?.let { subscription ->
                        viewModel.sendCreateSubscriptions()
                        subscriptionManager.launchBillingFlow(
                            requireActivity(),
                            subscription.productDetails
                        )
                    }
                }
            }
        }
    }

    private fun displayMainLayout(show: Boolean, subscription: Subscription) {
        val binding = binding ?: return
        val failedLayout = binding.failedLayout
        val mainLayout = binding.mainLayout
        val btnSubmit = binding.btnSubmit
        if (show) {
            failedLayout.visibility = View.INVISIBLE
            mainLayout.visibility = View.VISIBLE
            btnSubmit.text = getString(
                when (subscription) {
                    is Subscription.WithTrial -> LR.string.profile_start_free_trial
                    is Subscription.Simple -> LR.string.profile_confirm
                }
            )
        } else {
            mainLayout.visibility = View.INVISIBLE
            failedLayout.visibility = View.VISIBLE
            btnSubmit.text = getString(LR.string.profile_payment_try_again)
        }
    }
}
