package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingPlusFeatures.PlusOutlinedRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingPlusFeatures.PlusRowButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingPlusFeatures.plusGradientBrush
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingPlusBottomSheetState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingPlusBottomSheetViewModel
import au.com.shiftyjelly.pocketcasts.compose.components.Clickable
import au.com.shiftyjelly.pocketcasts.compose.components.ClickableTextHelper
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingPlusBottomSheet() {
    val viewModel = hiltViewModel<OnboardingPlusBottomSheetViewModel>()
    val state = viewModel.state.collectAsState().value
    val resources = LocalContext.current.resources

    when (state) {
        OnboardingPlusBottomSheetState.Loading -> {
            TextH20("PLACEHOLDER") // TODO
        }
        is OnboardingPlusBottomSheetState.Loaded -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color(0xFF282829))
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 40.dp)
            ) {
                TextH20(
                    text = stringResource(LR.string.onboarding_plus_become_a_plus_member),
                    textAlign = TextAlign.Center,
                    color = Color.White
                )

                Spacer(Modifier.height(8.dp))
                Box {
                    Box(
                        Modifier.background(
                            brush = plusGradientBrush,
                            shape = RoundedCornerShape(4.dp)
                        )
                    ) {
                        TextP60(
                            text = "PLACEHOLDER",
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 4.dp
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    state.subscriptions.forEach { subscription ->
                        PlusOutlinedRowButton(
                            text = subscription.recurringPricingPhase.pricePerPeriod(resources),
                            onClick = { /* TODO */ }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextP60(
                    text = "PLACEHOLDER",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                Divider(
                    thickness = 1.dp,
                    color = Color(0xFFE4E4E4),
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .alpha(0.24f)
                )

                PlusRowButton(
                    text = stringResource(LR.string.onboarding_plus_start_free_trial_and_subscribe),
                    onClick = {},
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
    }
}
