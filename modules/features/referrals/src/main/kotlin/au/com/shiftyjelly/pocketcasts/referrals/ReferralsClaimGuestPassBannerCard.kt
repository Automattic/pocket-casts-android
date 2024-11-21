package au.com.shiftyjelly.pocketcasts.referrals

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfoMock
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ReferralsClaimGuestPassBannerCard(
    state: UiState,
    onClick: () -> Unit,
    onHideBannerClick: () -> Unit,
    onBannerShown: () -> Unit,
    onShowReferralsSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CallOnce {
        onBannerShown()
    }
    LaunchedEffect(Unit) {
        onShowReferralsSheet()
    }

    when (state) {
        is UiState.Loading -> Unit
        is UiState.Loaded -> {
            ReferralsClaimGuestPassBannerCard(
                state = state,
                modifier = modifier,
                onClick = onClick,
                onHideBannerClick = onHideBannerClick,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReferralsClaimGuestPassBannerCard(
    state: UiState.Loaded,
    onClick: () -> Unit,
    onHideBannerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showProfileBanner) {
        var showPopupToHideBanner by remember { mutableStateOf(false) }
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showPopupToHideBanner = true },
                    onLongClickLabel = stringResource(LR.string.referrals_banner_long_press_label),
                ),
        ) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.theme.colors.primaryUi01Active, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val guestPassCardWidth = min(this@BoxWithConstraints.maxWidth / 4, 100.dp)
                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    val language = Locale.current.language
                    val textResId = if (language == "en") {
                        LR.string.referrals_claim_guess_pass_banner_card_title_english_only
                    } else {
                        LR.string.referrals_claim_guess_pass_banner_card_title
                    }
                    TextH40(
                        text = stringResource(
                            textResId,
                            requireNotNull(state.referralsOfferInfo).localizedOfferDurationAdjective,
                        ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextC70(
                        text = stringResource(LR.string.referrals_claim_guess_pass_banner_card_subtitle),
                        isUpperCase = false,
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                ReferralGuestPassCardView(
                    modifier = Modifier
                        .width(guestPassCardWidth)
                        .height(guestPassCardWidth * ReferralGuestPassCardDefaults.cardAspectRatio),
                    source = ReferralGuestPassCardViewSource.ProfileBanner,
                    referralsOfferInfo = requireNotNull(state.referralsOfferInfo),
                )
            }
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                DropdownMenu(
                    expanded = showPopupToHideBanner,
                    onDismissRequest = { showPopupToHideBanner = false },
                    offset = DpOffset(8.dp, -(32).dp),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable {
                            showPopupToHideBanner = false
                            onHideBannerClick()
                        },
                ) {
                    TextH40(stringResource(LR.string.referrals_banner_hide_label))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReferralsClaimGuestPassBannerCardPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        ReferralsClaimGuestPassBannerCard(
            state = UiState.Loaded(
                showProfileBanner = true,
                referralsOfferInfo = ReferralsOfferInfoMock,
            ),
            onClick = {},
            onHideBannerClick = {},
            onBannerShown = {},
            onShowReferralsSheet = {},
        )
    }
}
