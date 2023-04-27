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
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.PlusOutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UnselectedPlusOutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeBottomSheetState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeBottomSheetViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
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

    val userSignedInOrSignedUpInUpsellFlow = flow is OnboardingFlow.PlusUpsell &&
        (source == OnboardingUpgradeSource.RECOMMENDATIONS || source == OnboardingUpgradeSource.LOGIN)
    val startInExpandedState =
        // Only start with expanded state if there are any subscriptions
        hasSubscriptions && (
            // The hidden state is shown as the first screen in the PlusUpsell flow, so when we return
            // to this screen after login/signup we want to immediately expand the purchase bottom sheet.
            userSignedInOrSignedUpInUpsellFlow ||
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

    val activity = LocalContext.current.getActivity()
    @OptIn(ExperimentalMaterialApi::class)
    ModalBottomSheetLayout(
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        content = {
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
                        mainSheetViewModel.onClickSubscribe(
                            activity = activity,
                            flow = flow,
                            onComplete = onProceed,
                        )
                    } else {
                        LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, NULL_ACTIVITY_ERROR)
                    }
                },
                canUpgrade = hasSubscriptions,
            )
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
        PlusOutlinedRowButton(
            text = "one this is way too long | | | | | | | | | | |",
            selectedCheckMark = true,
            onClick = {},
        )
        PlusOutlinedRowButton(
            text = "two",
            topText = "woohoo!",
            selectedCheckMark = true,
            onClick = {},
        )
        UnselectedPlusOutlinedRowButton(
            text = "three",
            onClick = {},
        )
        UnselectedPlusOutlinedRowButton(
            text = "four this is also way too long | | | | | | |",
            topText = "woohoo!",
            onClick = {},
        )
    }
}
