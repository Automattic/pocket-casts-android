package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import android.content.res.Resources
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingPlusFeatures.PlusOutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingPlusFeatures.PlusRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingPlusFeatures.UnselectedPlusOutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingPlusBottomSheetState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingPlusBottomSheetViewModel
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.Pill
import au.com.shiftyjelly.pocketcasts.compose.components.Clickable
import au.com.shiftyjelly.pocketcasts.compose.components.ClickableTextHelper
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.models.type.TrialSubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import java.util.Locale
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingPlusBottomSheet(
    onClickSubscribe: () -> Unit,
) {

    // The keyboard sometimes gets opened when returning from the Google payment flow.
    // This is keeps it closed while on this screen.
    KeepKeyboardClosed()

    val viewModel = hiltViewModel<OnboardingPlusBottomSheetViewModel>()
    val state = viewModel.state.collectAsState().value
    val subscriptions = (state as? OnboardingPlusBottomSheetState.Loaded)?.subscriptions
        ?: emptyList()

    val resources = LocalContext.current.resources

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color(0xFF282829))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 40.dp)
    ) {

        Pill()

        Spacer(Modifier.height(32.dp))
        TextH20(
            text = stringResource(LR.string.onboarding_plus_become_a_plus_member),
            textAlign = TextAlign.Center,
            color = Color.White
        )

        if (state is OnboardingPlusBottomSheetState.Loaded) {
            Spacer(Modifier.height(16.dp))

            // Using LazyColumn instead of Column to avoid issue where unselected button that was not
            // being tapped would sometimes display the on-touch ripple effect
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                subscriptions.forEach { subscription ->

                    // Have to remember the interaction source here instead of inside the RowButtons
                    // because otherwise the interaction sources get misapplied to the wrong button
                    // as the user changes selections.
                    val interactionSource = remember(subscription) { MutableInteractionSource() }

                    val text = subscription.recurringPricingPhase.pricePerPeriod(resources)
                    val topText = subscription
                        .trialPricingPhase
                        ?.numPeriodFreeTrial(resources)
                        ?.uppercase(Locale.getDefault())

                    if (subscription == state.selectedSubscription) {
                        PlusOutlinedRowButton(
                            text = text,
                            topText = topText,
                            onClick = { viewModel.updateSelectedSubscription(subscription) },
                            interactionSource = interactionSource,
                            selectedCheckMark = true,
                        )
                    } else {
                        UnselectedPlusOutlinedRowButton(
                            text = text,
                            topText = topText,
                            onClick = { viewModel.updateSelectedSubscription(subscription) },
                            interactionSource = interactionSource,
                        )
                    }
                }
            }

            val descriptionText = state.selectedSubscription.trialPricingPhase.let { trialPhase ->
                if (trialPhase != null) {
                    stringResource(
                        LR.string.onboarding_plus_recurring_after_free_trial,
                        recurringAfterString(trialPhase, resources)
                    )
                } else {
                    val firstLine = stringResource(state.selectedSubscription.recurringPricingPhase.renews)
                    val secondLine = stringResource(LR.string.onboarding_plus_can_be_canceled_at_any_time)
                    "$firstLine.\n$secondLine"
                }
            }

            TextP60(
                text = descriptionText,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
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
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        Divider(
            thickness = 1.dp,
            color = Color(0xFFE4E4E4),
            modifier = Modifier
                .padding(vertical = 16.dp)
                .alpha(0.24f)
        )

        PlusRowButton(
            text = stringResource(LR.string.onboarding_plus_start_free_trial_and_subscribe),
            onClick = onClickSubscribe,
        )

        Spacer(Modifier.height(16.dp))

        val privacyPolicyText = stringResource(LR.string.onboarding_plus_privacy_policy)
        val termsAndConditionsText = stringResource(LR.string.onboarding_plus_terms_and_conditions)
        val text = stringResource(
            LR.string.onboarding_plus_continuing_agrees_to,
            privacyPolicyText,
            termsAndConditionsText
        )
        val uriHandler = LocalUriHandler.current
        ClickableTextHelper(
            text = text,
            color = Color.White,
            textAlign = TextAlign.Center,
            clickables = listOf(
                Clickable(
                    text = privacyPolicyText,
                    onClick = {
                        uriHandler.openUri(Settings.INFO_PRIVACY_URL)
                    }
                ),
                Clickable(
                    text = termsAndConditionsText,
                    onClick = {
                        uriHandler.openUri(Settings.INFO_TOS_URL)
                    }
                ),
            )
        )
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
    easing = EaseInOut
)

private fun recurringAfterString(
    trialSubscriptionPricingPhase: TrialSubscriptionPricingPhase,
    res: Resources
) = "${trialSubscriptionPricingPhase.numPeriodFreeTrial(res)} (${trialSubscriptionPricingPhase.trialEnd()})"
