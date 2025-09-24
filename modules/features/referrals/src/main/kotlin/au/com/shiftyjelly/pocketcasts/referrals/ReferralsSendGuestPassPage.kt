package au.com.shiftyjelly.pocketcasts.referrals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.adaptive.isAtLeastMediumHeight
import au.com.shiftyjelly.pocketcasts.compose.buttons.CloseButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.GradientRowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.extensions.plusBackgroundBrush
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.flatMap
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.referrals.ReferralPageDefaults.PAGE_WIDTH_PERCENT
import au.com.shiftyjelly.pocketcasts.referrals.ReferralPageDefaults.pageCornerRadius
import au.com.shiftyjelly.pocketcasts.referrals.ReferralPageDefaults.shouldShowFullScreen
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsSendGuestPassViewModel.ReferralSendGuestPassError
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsSendGuestPassViewModel.UiState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ReferralsSendGuestPassPage(
    onDismiss: () -> Unit,
    viewModel: ReferralsSendGuestPassViewModel = hiltViewModel(),
) {
    AppTheme(Theme.ThemeType.DARK) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val activity = LocalContext.current.getActivity()

        CallOnce {
            viewModel.onShown()
        }

        ReferralsSendGuestPassContent(
            state = state,
            onRetry = viewModel::onRetry,
            onDismiss = onDismiss,
            onShare = viewModel::onShareClick,
        )

        DisposableEffect(Unit) {
            onDispose {
                // Fragment will remain on orientation changes
                val fragmentRemoved = activity?.supportFragmentManager?.findFragmentByTag(ReferralsGuestPassFragment::class.java.name) == null
                if (fragmentRemoved) viewModel.onDispose()
            }
        }
    }
}

@Composable
private fun ReferralsSendGuestPassContent(
    state: UiState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onShare: (String, String, String) -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(Color.Transparent)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            )
            .fillMaxSize(),
    ) {
        val showFullScreen = shouldShowFullScreen(windowSizeClass)
        val pageWidth = if (showFullScreen) maxWidth else (maxWidth.value * PAGE_WIDTH_PERCENT).dp
        val pageModifier = if (showFullScreen) {
            Modifier
                .fillMaxSize()
        } else {
            Modifier
                .width(pageWidth)
                .wrapContentSize()
        }
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(pageCornerRadius(showFullScreen)),
            backgroundColor = Color.Black,
            modifier = pageModifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            when (state) {
                UiState.Loading ->
                    LoadingView(color = Color.White)

                is UiState.Loaded -> {
                    val offerName = state.referralPlan.offerName
                    val offerDuration = state.referralPlan.offerDurationText
                    SendGuestPassContent(
                        state = state,
                        showFullScreen = showFullScreen,
                        windowSizeClass = windowSizeClass,
                        pageWidth = pageWidth,
                        onDismiss = onDismiss,
                        onShare = { onShare(state.code, offerName, offerDuration) },
                    )
                }

                is UiState.Error -> {
                    val errorMessage = when (state.error) {
                        ReferralSendGuestPassError.Empty,
                        ReferralSendGuestPassError.FailedToLoad,
                        -> stringResource(LR.string.error_generic_message)

                        ReferralSendGuestPassError.NoNetwork -> stringResource(LR.string.error_no_network)
                    }
                    ReferralsGuestPassError(errorMessage, onRetry, onDismiss)
                }
            }
        }
    }
}

@Composable
private fun SendGuestPassContent(
    state: UiState.Loaded,
    showFullScreen: Boolean,
    windowSizeClass: WindowSizeClass,
    pageWidth: Dp,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        CloseButton(
            modifier = Modifier
                .align(Alignment.End),
            onClick = onDismiss,
        )

        SubscriptionBadge(
            fontSize = 16.sp,
            padding = 4.dp,
            iconRes = IR.drawable.ic_plus,
            shortNameRes = LR.string.pocket_casts_plus_short,
            iconColor = Color.Black,
            backgroundBrush = plusBackgroundBrush,
            textColor = Color.Black,
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextH10(
            text = stringResource(LR.string.referrals_send_guest_pass_title, state.referralPlan.offerDurationText),
            textAlign = TextAlign.Center,
        )

        if (windowSizeClass.isAtLeastMediumHeight()) {
            Spacer(modifier = Modifier.height(24.dp))

            ReferralsPassCardsStack(
                state = state,
                width = pageWidth,
            )
        }

        if (showFullScreen) {
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }

        GradientRowButton(
            primaryText = stringResource(LR.string.referrals_share_guest_pass),
            textColor = Color.Black,
            gradientBackgroundColor = plusBackgroundBrush,
            modifier = Modifier.padding(16.dp),
            onClick = onShare,
        )
    }
}

@Composable
private fun ReferralsPassCardsStack(
    state: UiState.Loaded,
    width: Dp,
    cardsCount: Int = 3,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .width(width),
    ) {
        (0..<cardsCount).reversed().forEach { index ->
            val cardWidth = (maxWidth.value * 0.8 * (1 - index * 0.125)).dp
            val cardHeight = (cardWidth.value * ReferralGuestPassCardDefaults.cardAspectRatio).dp
            val cardOffset = (10 * ((cardsCount - 1) - index)).dp
            ReferralGuestPassCardView(
                referralPlan = state.referralPlan,
                source = ReferralGuestPassCardViewSource.Send,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(cardWidth, cardHeight)
                    .offset(y = cardOffset),
            )
        }
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun ReferralsSendGuestPassPortraitPhonePreview() {
    ReferralsSendGuestPassContentPreview()
}

@Preview(device = Devices.LANDSCAPE_REGULAR)
@Composable
private fun ReferralsSendGuestPassLandscapePhonePreview() {
    ReferralsSendGuestPassContentPreview()
}

@Preview(device = Devices.PORTRAIT_TABLET)
@Composable
private fun ReferralsSendGuestPassPortraitTabletPreview() {
    ReferralsSendGuestPassContentPreview()
}

@Preview(device = Devices.LANDSCAPE_TABLET)
@Composable
private fun ReferralsSendGuestPassLandscapeTabletPreview() {
    ReferralsSendGuestPassContentPreview()
}

@Composable
fun ReferralsSendGuestPassContentPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        ReferralsSendGuestPassContent(
            state = UiState.Loaded(
                referralPlan = SubscriptionPlans.Preview
                    .findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.Referral)
                    .flatMap(ReferralSubscriptionPlan::create)
                    .getOrNull()!!,
                code = "",
            ),
            onDismiss = {},
            onShare = { _, _, _ -> },
            onRetry = {},
        )
    }
}
