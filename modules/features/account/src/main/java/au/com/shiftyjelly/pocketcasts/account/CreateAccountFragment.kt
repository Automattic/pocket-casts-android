package au.com.shiftyjelly.pocketcasts.account

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import au.com.shiftyjelly.pocketcasts.account.components.ProductAmountView
import au.com.shiftyjelly.pocketcasts.account.databinding.FragmentCreateAccountBinding
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.CreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SubscriptionType
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class CreateAccountFragment : BaseFragment() {

    @Inject lateinit var settings: Settings

    private val viewModel: CreateAccountViewModel by activityViewModels()

    private var selectedColor = 0
    private var unselectedBorderColor = 0
    private var unselectedTextColor = 0
    private var unselectedRadioColor = 0
    private var binding: FragmentCreateAccountBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCreateAccountBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        viewModel.loadSubs()

        setupForm()
        updateForm(viewModel.subscriptionType.value)

        binding.freeLayout.setOnClickListener {
            viewModel.updateSubscriptionType(SubscriptionType.FREE)
        }

        binding.plusLayout.setOnClickListener {
            viewModel.updateSubscriptionType(SubscriptionType.PLUS)
        }

        viewModel.createAccountState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CreateAccountState.ProductsLoaded -> {

                    val subscription = CreateAccountViewModel.defaultSubscription(state.list)
                    val plusLabel = when (subscription) {
                        is Subscription.Simple, null -> binding.root.resources.getString(LR.string.pocket_casts_plus)
                        is Subscription.WithTrial -> binding.root.resources.getString(LR.string.pocket_casts_plus_short)
                    }
                    binding.lblPlus.text = plusLabel

                    binding.chargeComposeView.apply {
                        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                        setContent {
                            AppTheme(theme.activeTheme) {

                                val emphasized = true
                                when (subscription) {
                                    is Subscription.Simple -> ProductAmountView(
                                        primaryText = subscription.recurringPricingPhase.formattedPrice,
                                        secondaryText = stringResource(subscription.recurringPricingPhase.perPeriod)
                                            .lowercase(Locale.getDefault()),
                                        horizontalAlignment = Alignment.End,
                                        emphasized = emphasized
                                    )
                                    is Subscription.WithTrial -> {
                                        val res = LocalContext.current.resources
                                        val primaryText = stringResource(
                                            LR.string.plus_trial_duration_free,
                                            subscription.trialPricingPhase.periodValue(res)
                                        )
                                        ProductAmountView(
                                            primaryText = primaryText,
                                            secondaryText = subscription.recurringPricingPhase.thenPriceSlashPeriod(res),
                                            horizontalAlignment = Alignment.End,
                                            emphasized = emphasized
                                        )
                                    }
                                    null -> { /* show nothing */ }
                                }
                            }
                        }
                    }
                }
                is CreateAccountState.CurrentlyValid -> {
                }
                is CreateAccountState.SubscriptionTypeChosen -> {
                    viewModel.subscriptionType.value?.let { subscriptionType ->
                        updateForm(subscriptionType)
                    }
                }
                else -> {}
            }
        }

        binding.lblFindMore.setOnClickListener {
            WebViewActivity.show(context, getString(LR.string.learn_more), Settings.INFO_LEARN_MORE_URL)
        }

        binding.btnNext.setOnClickListener {
            if (viewModel.subscriptionType.value == SubscriptionType.FREE) {
                it.findNavController().navigate(R.id.action_createAccountFragment_to_createEmailFragment)
            } else if (viewModel.subscriptionType.value == SubscriptionType.PLUS) {
                it.findNavController().navigate(R.id.action_createAccountFragment_to_createFrequencyFragment)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.currentFocus?.let { view ->
            UiUtil.hideKeyboard(view)
        }
    }

    private fun setupForm() {
        val binding = binding ?: return
        val context = binding.root.context

        binding.lblUnlockedText.text = getText(LR.string.plus_create_marketing_features)
        binding.lblFeature3.text = getString(LR.string.plus_cloud_storage_limit, settings.getCustomStorageLimitGb())

        selectedColor = context.getThemeColor(UR.attr.primary_field_03_active)
        unselectedBorderColor = context.getThemeColor(UR.attr.primary_field_03)
        unselectedTextColor = context.getThemeColor(UR.attr.primary_text_01)
        unselectedRadioColor = context.getThemeColor(UR.attr.primary_icon_02)
    }

    private fun updateForm(subscriptionType: SubscriptionType?) {
        val freeSub = (subscriptionType != null && subscriptionType == SubscriptionType.FREE)
        val plusSub = (subscriptionType != null && subscriptionType == SubscriptionType.PLUS)

        val binding = binding ?: return

        val btnFree = binding.btnFree
        btnFree.buttonTintList = if (freeSub) ColorStateList.valueOf(selectedColor) else ColorStateList.valueOf(unselectedRadioColor)
        val btnPlus = binding.btnPlus
        btnPlus.buttonTintList = if (plusSub) ColorStateList.valueOf(selectedColor) else ColorStateList.valueOf(unselectedRadioColor)

        btnFree.isChecked = freeSub
        binding.lblRegular.setTextColor(if (freeSub) selectedColor else unselectedTextColor)
        btnPlus.isChecked = plusSub
        binding.lblPlus.setTextColor(if (plusSub) selectedColor else unselectedTextColor)

        binding.outlinePanel.setSelectedWithColors(btnFree.isChecked, selectedColor, unselectedBorderColor, 4)
        binding.plusPanel.setSelectedWithColors(btnPlus.isChecked, selectedColor, unselectedBorderColor, 4)

        binding.btnNext.isEnabled = subscriptionType != null
    }
}
