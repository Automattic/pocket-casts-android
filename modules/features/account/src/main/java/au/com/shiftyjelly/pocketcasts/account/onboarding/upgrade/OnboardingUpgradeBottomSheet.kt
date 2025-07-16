package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import android.view.ViewTreeObserver
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.OutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UnselectedOutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper.UpgradeRowButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.Pill
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.patronGradientBrush
import au.com.shiftyjelly.pocketcasts.compose.plusGradientBrush
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingUpgradeBottomSheet(
    viewModel: OnboardingUpgradeFeaturesViewModel,
    state: OnboardingUpgradeFeaturesState,
    onClickSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
    onPrivacyPolicyClick: () -> Unit = {},
    onTermsAndConditionsClick: () -> Unit = {},
) {
    // The keyboard sometimes gets opened when returning from the Google payment flow.
    // This is keeps it closed while on this screen.
    KeepKeyboardClosed()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Color(0xFF282829))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 40.dp),
    ) {
        Pill()

        Spacer(Modifier.height(32.dp))

        if (state is OnboardingUpgradeFeaturesState.Loaded) {
            TextH20(
                text = stringResource(state.selectedTier.toSubscribeTitle()),
                textAlign = TextAlign.Center,
                color = Color.White,
            )

            Spacer(Modifier.height(16.dp))

            // Using LazyColumn instead of Column to avoid issue where unselected button that was not
            // being tapped would sometimes display the on-touch ripple effect
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val subscriptionPlans = remember(state.selectedPlan, state.availablePlans) {
                    state.availablePlans.filter { it.key.tier == state.selectedTier }
                }

                subscriptionPlans.forEach { plan ->
                    // Have to remember the interaction source here instead of inside the RowButtons
                    // because otherwise the interaction sources get misapplied to the wrong button
                    // as the user changes selections.
                    val interactionSource = remember(plan) { MutableInteractionSource() }

                    Column {
                        val offerText = plan.offerBadgeText?.uppercase()
                        if (offerText == null) {
                            Spacer(Modifier.height(8.dp))
                        }

                        if (plan == state.selectedPlan) {
                            OutlinedRowButton(
                                text = plan.pricePerPeriodText,
                                topText = offerText,
                                subscriptionTier = plan.key.tier,
                                brush = plan.key.tier.toButtonBackgroundBrush(),
                                onClick = {
                                    viewModel.changeBillingCycle(plan.key.billingCycle)
                                    viewModel.changeSubscriptionTier(plan.key.tier)
                                },
                                interactionSource = interactionSource,
                                selectedCheckMark = true,
                            )
                        } else {
                            UnselectedOutlinedRowButton(
                                text = plan.pricePerPeriodText,
                                topText = offerText,
                                subscriptionTier = plan.key.tier,
                                onClick = {
                                    viewModel.changeBillingCycle(plan.key.billingCycle)
                                    viewModel.changeSubscriptionTier(plan.key.tier)
                                },
                                interactionSource = interactionSource,
                            )
                        }
                    }
                }
            }

            TextP60(
                text = state.selectedPlan.planDescriptionText,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )

            AnimatedVisibility(
                visible = state.purchaseFailed,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = animationSpec,
                ),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = animationSpec,
                ),
            ) {
                TextP60(
                    text = stringResource(LR.string.profile_create_subscription_failed),
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Divider(
                thickness = 1.dp,
                color = Color(0xFFE4E4E4),
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .alpha(0.24f),
            )

            UpgradeRowButton(
                primaryText = state.selectedTier.toSubscribeButtonText(),
                gradientBackgroundColor = state.selectedTier.toButtonBackgroundBrush(),
                textColor = state.selectedTier.toButtonTextColor(),
                onClick = onClickSubscribe,
            )
        }

        Spacer(Modifier.height(16.dp))

        OnboardingUpgradeHelper.PrivacyPolicy(
            color = Color.White,
            textAlign = TextAlign.Center,
            onPrivacyPolicyClick = onPrivacyPolicyClick,
            onTermsAndConditionsClick = onTermsAndConditionsClick,
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun KeepKeyboardClosed() {
    val view = LocalView.current
    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val isKeyboardOpen = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) ?: true
            if (isKeyboardOpen) {
                UiUtil.hideKeyboard(view)
            }
        }

        with(view.viewTreeObserver) {
            addOnGlobalLayoutListener(listener)
            onDispose {
                removeOnGlobalLayoutListener(listener)
            }
        }
    }
}

private val animationSpec = tween<IntSize>(
    durationMillis = 600,
    easing = EaseInOut,
)

private fun SubscriptionTier.toSubscribeTitle() = when (this) {
    SubscriptionTier.Plus -> LR.string.onboarding_subscribe_to_plus
    SubscriptionTier.Patron -> LR.string.onboarding_patron_subscribe
}

@Composable
private fun SubscriptionTier.toSubscribeButtonText() = stringResource(
    LR.string.subscribe_to,
    when (this) {
        SubscriptionTier.Plus -> stringResource(LR.string.pocket_casts_plus_short)
        SubscriptionTier.Patron -> stringResource(LR.string.pocket_casts_patron_short)
    },
)

private fun SubscriptionTier.toButtonBackgroundBrush() = when (this) {
    SubscriptionTier.Plus -> Brush.plusGradientBrush
    SubscriptionTier.Patron -> Brush.patronGradientBrush
}

private fun SubscriptionTier.toButtonTextColor() = when (this) {
    SubscriptionTier.Plus -> Color.Black
    SubscriptionTier.Patron -> Color.White
}
