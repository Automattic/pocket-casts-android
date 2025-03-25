package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.OutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UnselectedOutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeBottomSheetState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeBottomSheetViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.compose.plusGradientBrush
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.launch

private const val NULL_ACTIVITY_ERROR = "Activity is null when attempting subscription"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingUpgradeFlow(
    flow: OnboardingFlow,
    source: OnboardingUpgradeSource,
    isLoggedIn: Boolean,
    forcePurchase: Boolean,
    onBackPressed: () -> Unit,
    onNeedLogin: () -> Unit,
    onProceed: () -> Unit,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
) {
    val bottomSheetViewModel = hiltViewModel<OnboardingUpgradeBottomSheetViewModel>()
    val mainSheetViewModel = hiltViewModel<OnboardingUpgradeFeaturesViewModel>()
    val state = bottomSheetViewModel.state.collectAsState().value
    val hasSubscriptions = state is OnboardingUpgradeBottomSheetState.Loaded && state.subscriptions.isNotEmpty()

    val coroutineScope = rememberCoroutineScope()
    val activity = LocalContext.current.getActivity()

    val forceAutoPurchase = forcePurchase ||
        (flow is OnboardingFlow.Upsell && (source == OnboardingUpgradeSource.RECOMMENDATIONS || source == OnboardingUpgradeSource.LOGIN))

    if (forceAutoPurchase) {
        activity?.let {
            LaunchedEffect(Unit) {
                mainSheetViewModel.onClickSubscribe(
                    activity = activity,
                    flow = flow,
                    source = source,
                    onComplete = onProceed,
                )
            }
        }
    }

    val startSelectPaymentFrequencyInExpandedState =
        // Only start with expanded state if there are any subscriptions and payment frequency selection is needed
        hasSubscriptions && (
            flow is OnboardingFlow.PlusAccountUpgradeNeedsLogin ||
                flow is OnboardingFlow.PlusAccountUpgrade
            )
    val initialValue = if (startSelectPaymentFrequencyInExpandedState) {
        SheetValue.Expanded
    } else {
        SheetValue.Hidden
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true },
    )

    LaunchedEffect(sheetState.targetValue) {
        when (sheetState.targetValue) {
            SheetValue.Hidden -> {
                // Don't fire event when initially loading the screen and both current and target are "Hidden"
                if (sheetState.currentValue == SheetValue.Expanded) {
                    bottomSheetViewModel.onSelectPaymentFrequencyDismissed(flow, source)
                    if (flow is OnboardingFlow.PlusAccountUpgrade) {
                        mainSheetViewModel.onDismiss(flow, source)
                        onBackPressed()
                    }
                }
            }
            SheetValue.Expanded -> bottomSheetViewModel.onSelectPaymentFrequencyShown(flow, source)
            else -> {}
        }
    }

    LaunchedEffect(sheetState.currentValue) {
        // We need to check if the screen was initialized with the expanded state.
        // Otherwise, the sheet will never be shown since the initial state is Hidden.
        // This will trigger this event, and onBackPressed will be called.
        if (sheetState.currentValue == SheetValue.Hidden && startSelectPaymentFrequencyInExpandedState) {
            onBackPressed()
        }
    }

    BackHandler {
        if (sheetState.isVisible) {
            coroutineScope.launch { sheetState.hide() }
        } else {
            mainSheetViewModel.onDismiss(flow, source)
            onBackPressed()
        }
    }

    if (sheetState.isVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch { sheetState.hide() }
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            content = {
                OnboardingUpgradeBottomSheet(
                    onClickSubscribe = {
                        if (activity != null) {
                            bottomSheetViewModel.onClickSubscribe(
                                activity = activity,
                                flow = flow,
                                source = source,
                                onComplete = onProceed,
                            )
                        } else {
                            LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, NULL_ACTIVITY_ERROR)
                        }
                    },
                    onPrivacyPolicyClick = {
                        mainSheetViewModel.onPrivacyPolicyPressed()
                    },
                    onTermsAndConditionsClick = {
                        mainSheetViewModel.onTermsAndConditionsPressed()
                    },
                )
            },
        )
    }

    if (flow !is OnboardingFlow.PlusAccountUpgrade) {
        OnboardingUpgradeFeaturesPage(
            flow = flow,
            source = source,
            onBackPressed = onBackPressed,
            onClickSubscribe = { showUpgradeBottomSheet ->
                if (activity != null) {
                    if (isLoggedIn) {
                        if (showUpgradeBottomSheet) {
                            coroutineScope.launch {
                                sheetState.show()
                            }
                        } else {
                            mainSheetViewModel.onClickSubscribe(
                                activity = activity,
                                flow = flow,
                                source = source,
                                onComplete = onProceed,
                            )
                        }
                    } else {
                        onNeedLogin()
                    }
                } else {
                    LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, NULL_ACTIVITY_ERROR)
                }
            },
            onNotNowPressed = onProceed,
            canUpgrade = hasSubscriptions,
            onUpdateSystemBars = onUpdateSystemBars,
        )
    }
}

@Preview
@Composable
private fun OutlinedButtonPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedRowButton(
            text = "one this is way too long | | | | | | | | | | |",
            brush = Brush.plusGradientBrush,
            selectedCheckMark = true,
            subscriptionTier = SubscriptionTier.PLUS,
            onClick = {},
        )
        OutlinedRowButton(
            text = "two",
            topText = "woohoo!",
            brush = Brush.plusGradientBrush,
            subscriptionTier = SubscriptionTier.PLUS,
            selectedCheckMark = true,
            onClick = {},
        )
        UnselectedOutlinedRowButton(
            text = "three",
            subscriptionTier = SubscriptionTier.PLUS,
            onClick = {},
        )
        UnselectedOutlinedRowButton(
            text = "four this is also way too long | | | | | | |",
            topText = "woohoo!",
            subscriptionTier = SubscriptionTier.PLUS,
            onClick = {},
        )
    }
}
