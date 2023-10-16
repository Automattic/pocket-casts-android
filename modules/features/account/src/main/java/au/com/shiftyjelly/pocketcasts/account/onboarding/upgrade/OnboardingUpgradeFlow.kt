package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
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
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.launch

private const val NULL_ACTIVITY_ERROR = "Activity is null when attempting subscription"
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OnboardingUpgradeFlow(
    flow: OnboardingFlow,
    source: OnboardingUpgradeSource,
    isLoggedIn: Boolean,
    onBackPressed: () -> Unit,
    onNeedLogin: () -> Unit,
    onProceed: () -> Unit,
) {

    val bottomSheetViewModel = hiltViewModel<OnboardingUpgradeBottomSheetViewModel>()
    val mainSheetViewModel = hiltViewModel<OnboardingUpgradeFeaturesViewModel>()
    val state = bottomSheetViewModel.state.collectAsState().value
    val hasSubscriptions = state is OnboardingUpgradeBottomSheetState.Loaded && state.subscriptions.isNotEmpty()

    val coroutineScope = rememberCoroutineScope()
    val activity = LocalContext.current.getActivity()

    val userSignedInOrSignedUpInUpsellFlow = flow is OnboardingFlow.Upsell &&
        (source == OnboardingUpgradeSource.RECOMMENDATIONS || source == OnboardingUpgradeSource.LOGIN)

    if (FeatureFlag.isEnabled(Feature.ADD_PATRON_ENABLED) && userSignedInOrSignedUpInUpsellFlow) {
        activity?.let {
            LaunchedEffect(Unit) {
                mainSheetViewModel.onClickSubscribe(
                    activity = activity,
                    flow = flow,
                    onComplete = onProceed
                )
            }
        }
    }

    val startInExpandedState =
        // Only start with expanded state if there are any subscriptions
        hasSubscriptions && (
            // The hidden state is shown as the first screen in the Upsell flow, so when we return
            // to this screen after login/signup we want to immediately expand the purchase bottom sheet.
            (!FeatureFlag.isEnabled(Feature.ADD_PATRON_ENABLED) && userSignedInOrSignedUpInUpsellFlow) ||
                // User already indicated they want to upgrade, so go straight to purchase modal
                flow is OnboardingFlow.PlusAccountUpgradeNeedsLogin ||
                flow is OnboardingFlow.PlusAccountUpgrade
            )
    val initialValue = if (startInExpandedState) {
        ModalBottomSheetValue.Expanded
    } else {
        ModalBottomSheetValue.Hidden
    }
    val sheetState = rememberModalBottomSheetState(
        initialValue = initialValue,
        skipHalfExpanded = true,
    )

    LaunchedEffect(sheetState.targetValue) {
        when (sheetState.targetValue) {
            ModalBottomSheetValue.Hidden -> {
                // Don't fire event when initially loading the screen and both current and target are "Hidden"
                if (sheetState.currentValue == ModalBottomSheetValue.Expanded) {
                    bottomSheetViewModel.onSelectPaymentFrequencyDismissed(flow)
                    if (flow is OnboardingFlow.PlusAccountUpgrade) {
                        mainSheetViewModel.onDismiss(flow, source)
                        onBackPressed()
                    }
                }
            }
            ModalBottomSheetValue.Expanded -> bottomSheetViewModel.onSelectPaymentFrequencyShown(flow)
            else -> {}
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

    @OptIn(ExperimentalMaterialApi::class)
    ModalBottomSheetLayout(
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        content = {
            if (flow !is OnboardingFlow.PlusAccountUpgrade) {
                OnboardingUpgradeFeaturesPage(
                    flow = flow,
                    source = source,
                    onUpgradePressed = {
                        if (isLoggedIn) {
                            coroutineScope.launch { sheetState.show() }
                        } else {
                            onNeedLogin()
                        }
                    },
                    onNotNowPressed = onProceed,
                    onBackPressed = onBackPressed,
                    onClickSubscribe = {
                        if (activity != null) {
                            if (isLoggedIn) {
                                mainSheetViewModel.onClickSubscribe(
                                    activity = activity,
                                    flow = flow,
                                    onComplete = onProceed,
                                )
                            } else {
                                onNeedLogin()
                            }
                        } else {
                            LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, NULL_ACTIVITY_ERROR)
                        }
                    },
                    canUpgrade = hasSubscriptions,
                )
            }
        },
        sheetContent = {
            OnboardingUpgradeBottomSheet(
                onClickSubscribe = {
                    if (activity != null) {
                        bottomSheetViewModel.onClickSubscribe(
                            activity = activity,
                            flow = flow,
                            onComplete = onProceed,
                        )
                    } else {
                        LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, NULL_ACTIVITY_ERROR)
                    }
                }
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
            brush = OnboardingUpgradeHelper.plusGradientBrush,
            selectedCheckMark = true,
            subscriptionTier = Subscription.SubscriptionTier.PLUS,
            onClick = {},
        )
        OutlinedRowButton(
            text = "two",
            topText = "woohoo!",
            brush = OnboardingUpgradeHelper.plusGradientBrush,
            subscriptionTier = Subscription.SubscriptionTier.PLUS,
            selectedCheckMark = true,
            onClick = {},
        )
        UnselectedOutlinedRowButton(
            text = "three",
            subscriptionTier = Subscription.SubscriptionTier.PLUS,
            onClick = {},
        )
        UnselectedOutlinedRowButton(
            text = "four this is also way too long | | | | | | |",
            topText = "woohoo!",
            subscriptionTier = Subscription.SubscriptionTier.PLUS,
            onClick = {},
        )
    }
}
