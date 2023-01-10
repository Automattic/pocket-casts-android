package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingPlusFeatures.PlusOutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingPlusFeatures.UnselectedPlusOutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingPlusBottomSheetState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingPlusBottomSheetViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingPlusFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.extensions.brush
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OnboardingPlusUpgradeFlow(
    flow: OnboardingFlow,
    source: OnboardingUpgradeSource,
    isLoggedIn: Boolean,
    onBackPressed: () -> Unit,
    onNeedLogin: () -> Unit,
    onProceed: () -> Unit,
) {

    val bottomSheetViewModel = hiltViewModel<OnboardingPlusBottomSheetViewModel>()
    val mainSheetViewModel = hiltViewModel<OnboardingPlusFeaturesViewModel>()
    val state = bottomSheetViewModel.state.collectAsState().value
    val hasSubscriptions = state is OnboardingPlusBottomSheetState.Loaded && state.subscriptions.isNotEmpty()

    val coroutineScope = rememberCoroutineScope()

    val isPresentingSecondTime = flow is OnboardingFlow.PlusUpsell && source == OnboardingUpgradeSource.RECOMMENDATIONS
    val initialValue = when {
        // The hidden state is shown as the first screen in the PlusUpsell flow, so when we return
        // to this screen after login/signup we want to immediately expand the purchase bottom sheet.
        isPresentingSecondTime ||
            // User already indicated they want to upgrade, so go straight to purchase modal
            flow is OnboardingFlow.PlusAccountUpgradeNeedsLogin ||
            flow is OnboardingFlow.PlusAccountUpgrade -> {
            ModalBottomSheetValue.Expanded
        }
        else -> {
            ModalBottomSheetValue.Hidden
        }
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
            OnboardingPlusFeaturesPage(
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
                canUpgrade = hasSubscriptions,
            )
        },
        sheetContent = {
            OnboardingPlusBottomSheet(
                onClickSubscribe = {
                    if (activity != null) {
                        bottomSheetViewModel.onClickSubscribe(
                            activity = activity,
                            flow = flow,
                            onComplete = onProceed,
                        )
                    } else {
                        LogBuffer.e(
                            LogBuffer.TAG_SUBSCRIPTIONS,
                            "Activity is null when attempting subscription"
                        )
                    }
                }
            )
        },
    )
}

object OnboardingPlusFeatures {
    val plusGradientBrush = Brush.horizontalGradient(
        0f to Color(0xFFFED745),
        1f to Color(0xFFFEB525),
    )
    val unselectedColor = Color(0xFF666666)

    @Composable
    fun PlusRowButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(all = 0.dp), // Remove content padding
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(plusGradientBrush)
            ) {
                Text(
                    text = text,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(6.dp)
                        // add extra 8.dp extra padding to offset removal of button padding (see ButtonDefaults.ButtonVerticalPadding)
                        .padding(8.dp)
                        .align(Alignment.Center),
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
            }
        }
    }

    @Composable
    fun PlusOutlinedRowButton(
        text: String,
        topText: String? = null,
        onClick: () -> Unit,
        selectedCheckMark: Boolean = false,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        modifier: Modifier = Modifier,
    ) {

        ConstraintLayout(modifier) {

            val buttonRef = createRef()
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, plusGradientBrush),
                elevation = null,
                interactionSource = interactionSource,
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                modifier = Modifier.constrainAs(buttonRef) {
                    bottom.linkTo(parent.bottom)
                },
            ) {

                Box(Modifier.fillMaxWidth()) {
                    TextH30(
                        text = text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(vertical = 6.dp, horizontal = 24.dp)
                            .align(Alignment.Center)
                            .brush(plusGradientBrush)
                    )
                    if (selectedCheckMark) {
                        Icon(
                            painter = painterResource(IR.drawable.plus_check),
                            contentDescription = null,
                            modifier = Modifier
                                .brush(plusGradientBrush)
                                .align(Alignment.CenterEnd)
                                .width(24.dp)
                        )
                    }
                }
            }

            if (topText != null) {
                TopText(
                    buttonRef = buttonRef,
                    topText = topText,
                    selected = true
                )
            }
        }
    }
    @Composable
    fun UnselectedPlusOutlinedRowButton(
        text: String,
        topText: String? = null,
        onClick: () -> Unit,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        modifier: Modifier = Modifier,
    ) {
        ConstraintLayout(modifier) {

            val buttonRef = createRef()
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, unselectedColor),
                elevation = null,
                interactionSource = interactionSource,
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                modifier = Modifier.constrainAs(buttonRef) {
                    bottom.linkTo(parent.bottom)
                },
            ) {
                TextH30(
                    text = text,
                    textAlign = TextAlign.Center,
                    color = unselectedColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 24.dp)
                )
            }

            if (topText != null) {
                TopText(
                    buttonRef = buttonRef,
                    topText = topText,
                    selected = false
                )
            }
        }
    }

    @Composable
    private fun ConstraintLayoutScope.TopText(
        buttonRef: ConstrainedLayoutReference,
        topText: String,
        selected: Boolean,
    ) {
        val modifier = if (selected) {
            Modifier.background(
                brush = plusGradientBrush,
                shape = RoundedCornerShape(4.dp)
            )
        } else {
            Modifier.background(
                color = unselectedColor,
                shape = RoundedCornerShape(4.dp)
            )
        }

        val topTextRef = createRef()
        Box(
            modifier.constrainAs(topTextRef) {
                top.linkTo(buttonRef.top)
                bottom.linkTo(buttonRef.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ) {
            TextP60(
                text = topText,
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 2.dp
                ),
            )
        }
    }
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
