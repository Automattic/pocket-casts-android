package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.OutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UnselectedOutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.compose.plusGradientBrush
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.launch

private const val NULL_ACTIVITY_ERROR = "Activity is null when attempting subscription"

@Composable
fun OnboardingUpgradeFlow(
    viewModel: OnboardingUpgradeFeaturesViewModel,
    state: OnboardingUpgradeFeaturesState,
    flow: OnboardingFlow,
    source: OnboardingUpgradeSource,
    isLoggedIn: Boolean,
    forcePurchase: Boolean,
    onBackPress: () -> Unit,
    onNeedLogin: () -> Unit,
    onProceed: () -> Unit,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
) {
    val activity = LocalContext.current.getActivity()
    val coroutineScope = rememberCoroutineScope()

    val areSubscriptionsLoaded = state is OnboardingUpgradeFeaturesState.Loaded
    val forceAutoPurchase = forcePurchase ||
        (flow is OnboardingFlow.Upsell && (source == OnboardingUpgradeSource.RECOMMENDATIONS || source == OnboardingUpgradeSource.LOGIN))

    if (forceAutoPurchase) {
        activity?.let {
            LaunchedEffect(Unit) {
                viewModel.purchaseSelectedPlan(activity, onProceed)
            }
        }
    }

    // Only start with expanded state if there are any subscriptions and payment frequency selection is needed
    val startSelectPaymentFrequencyInExpandedState = areSubscriptionsLoaded &&
        (flow is OnboardingFlow.PlusAccountUpgradeNeedsLogin || flow is OnboardingFlow.PlusAccountUpgrade)
    val initialValue = if (startSelectPaymentFrequencyInExpandedState) {
        ModalBottomSheetValue.Expanded
    } else {
        ModalBottomSheetValue.Hidden
    }
    val sheetState = rememberModalBottomSheetState(
        initialValue = initialValue,
        skipHalfExpanded = true,
    )

    LaunchedEffect(sheetState.targetValue, onBackPress) {
        when (sheetState.targetValue) {
            ModalBottomSheetValue.Hidden -> {
                // Don't fire event when initially loading the screen and both current and target are "Hidden"
                if (sheetState.currentValue == ModalBottomSheetValue.Expanded) {
                    viewModel.onSelectPaymentFrequencyDismissed(flow, source)
                    if (flow is OnboardingFlow.PlusAccountUpgrade) {
                        viewModel.onDismiss(flow, source)
                        onBackPress()
                    }
                }
            }

            ModalBottomSheetValue.Expanded -> viewModel.onSelectPaymentFrequencyShown(flow, source)
            else -> {}
        }
    }

    LaunchedEffect(sheetState.currentValue, onBackPress) {
        // We need to check if the screen was initialized with the expanded state.
        // Otherwise, the sheet will never be shown since the initial state is Hidden.
        // This will trigger this event, and onBackPressed will be called.
        if (sheetState.currentValue == ModalBottomSheetValue.Hidden && startSelectPaymentFrequencyInExpandedState) {
            onBackPress()
        }
    }

    BackHandler {
        if (sheetState.isVisible) {
            coroutineScope.launch { sheetState.hide() }
        } else {
            viewModel.onDismiss(flow, source)
            onBackPress()
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        content = {
            if (flow !is OnboardingFlow.PlusAccountUpgrade) {
                OnboardingUpgradeFeaturesPage(
                    viewModel = @Suppress("ktlint:compose:vm-forwarding-check") viewModel,
                    state = state,
                    flow = flow,
                    source = source,
                    onBackPress = onBackPress,
                    onClickSubscribe = { showUpgradeBottomSheet ->
                        if (activity != null) {
                            if (isLoggedIn) {
                                if (showUpgradeBottomSheet) {
                                    coroutineScope.launch {
                                        sheetState.show()
                                    }
                                } else {
                                    viewModel.purchaseSelectedPlan(activity, onProceed)
                                }
                            } else {
                                onNeedLogin()
                            }
                        } else {
                            LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, NULL_ACTIVITY_ERROR)
                        }
                    },
                    onNotNowPress = onProceed,
                    onUpdateSystemBars = onUpdateSystemBars,
                )
            }
        },
        sheetContent = {
            OnboardingUpgradeBottomSheet(
                viewModel = @Suppress("ktlint:compose:vm-forwarding-check") viewModel,
                state = state,
                onClickSubscribe = {
                    if (activity != null) {
                        viewModel.purchaseSelectedPlan(activity, onProceed)
                    } else {
                        LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, NULL_ACTIVITY_ERROR)
                    }
                },
                onPrivacyPolicyClick = {
                    viewModel.onPrivacyPolicyPressed()
                },
                onTermsAndConditionsClick = {
                    viewModel.onTermsAndConditionsPressed()
                },
            )
        },
    )
}

@Preview
@Composable
private fun OutlinedButtonPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedRowButton(
            text = "one this is way too long | | | | | | | | | | |",
            brush = Brush.plusGradientBrush,
            selectedCheckMark = true,
            subscriptionTier = SubscriptionTier.Plus,
            onClick = {},
        )
        OutlinedRowButton(
            text = "two",
            topText = "woohoo!",
            brush = Brush.plusGradientBrush,
            subscriptionTier = SubscriptionTier.Plus,
            selectedCheckMark = true,
            onClick = {},
        )
        UnselectedOutlinedRowButton(
            text = "three",
            subscriptionTier = SubscriptionTier.Plus,
            onClick = {},
        )
        UnselectedOutlinedRowButton(
            text = "four this is also way too long | | | | | | |",
            topText = "woohoo!",
            subscriptionTier = SubscriptionTier.Plus,
            onClick = {},
        )
    }
}
