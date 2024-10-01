package au.com.shiftyjelly.pocketcasts.referrals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfoMock
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsGuestPassFragment.ReferralsPageType
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ReferralsClaimGuestPassBannerCard(
    modifier: Modifier = Modifier,
    viewModel: ReferralsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current.getActivity()

    when (state) {
        UiState.Loading -> Unit
        is UiState.Loaded -> {
            val loadedState = state as UiState.Loaded
            ReferralsClaimGuestPassBannerCard(
                state = loadedState,
                modifier = modifier,
                onClick = {
                    val fragment = ReferralsGuestPassFragment.newInstance(ReferralsPageType.Claim)
                    (activity as FragmentHostListener).showBottomSheet(fragment)
                },
            )
        }
    }

    activity?.supportFragmentManager?.findFragmentByTag(ReferralsGuestPassFragment::class.java.name)?.let {
        (activity as FragmentHostListener).showBottomSheet(it)
    }
}

@Composable
private fun ReferralsClaimGuestPassBannerCard(
    state: UiState.Loaded,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    if (state.showProfileBanner) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    onClick = onClick,
                ),
        ) {
            Row(
                modifier = modifier
                    .background(MaterialTheme.theme.colors.primaryUi01Active, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val guestPassCardWidth = min(this@BoxWithConstraints.maxWidth / 4, 100.dp)
                Column(
                    modifier = modifier
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

                    Spacer(modifier = modifier.height(8.dp))

                    TextC70(
                        text = stringResource(LR.string.referrals_claim_guess_pass_banner_card_subtitle),
                        isUpperCase = false,
                    )
                }

                Spacer(modifier = modifier.width(16.dp))

                ReferralGuestPassCardView(
                    modifier = modifier
                        .width(guestPassCardWidth)
                        .height(guestPassCardWidth * ReferralGuestPassCardDefaults.cardAspectRatio),
                    source = ReferralGuestPassCardViewSource.ProfileBanner,
                    referralsOfferInfo = requireNotNull(state.referralsOfferInfo),
                )
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
        )
    }
}
