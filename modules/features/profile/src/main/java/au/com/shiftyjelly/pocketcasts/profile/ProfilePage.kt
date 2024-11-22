package au.com.shiftyjelly.pocketcasts.profile

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.OrientationPreview
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.endofyear.ui.EndOfYearPromptCard
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfoMock
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsClaimGuestPassBannerCard
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsIconWithTooltip
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun ProfilePage(
    state: ProfilePageState,
    themeType: Theme.ThemeType,
    onSendReferralsClick: () -> Unit,
    onReferralsTooltipClick: () -> Unit,
    onReferralsTooltipShown: () -> Unit,
    onSettingsClick: () -> Unit,
    onHeaderClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onClaimReferralsClick: () -> Unit,
    onHideReferralsCardClick: () -> Unit,
    onReferralsCardShown: () -> Unit,
    onShowReferralsSheet: () -> Unit,
    onSectionClick: (ProfileSection) -> Unit,
    onRefreshClick: () -> Unit,
    onUpgradeProfileClick: () -> Unit,
    onCloseUpgradeProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppTheme(themeType) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.theme.colors.primaryUi02)
                .verticalScroll(rememberScrollState()),
        ) {
            Toolbar(
                showReferralsIcon = state.isSendReferralsEnabled,
                state = state.referralsState,
                onSendReferralsClick = onSendReferralsClick,
                onReferralsTooltipClick = onReferralsTooltipClick,
                onReferralsTooltipShown = onReferralsTooltipShown,
                onSettingsClick = onSettingsClick,
            )
            VerticalSpacer()
            HeaderWithStats(
                headerState = state.headerState,
                statsState = state.statsState,
                onHeaderClick = onHeaderClick,
            )
            VerticalSpacer()
            if (state.isPlaybackEnabled) {
                EndOfYearPromptCard(
                    onClick = onPlaybackClick,
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                )
                VerticalSpacer()
            }
            if (state.isClaimReferralsEnabled) {
                ReferralsClaimGuestPassBannerCard(
                    state = state.referralsState,
                    onClick = onClaimReferralsClick,
                    onHideBannerClick = onHideReferralsCardClick,
                    onBannerShown = onReferralsCardShown,
                    onShowReferralsSheet = onShowReferralsSheet,
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                )
                if ((state.referralsState as? ReferralsViewModel.UiState.Loaded)?.showProfileBanner == true) {
                    VerticalSpacer()
                }
            }
            HorizontalDivider()
            ProfileSections(
                sections = ProfileSection.entries,
                onClick = onSectionClick,
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpacer()
            var localRefreshState by remember(state.refreshState) { mutableStateOf(state.refreshState) }
            RefreshSection(
                refreshState = localRefreshState,
                onClick = {
                    localRefreshState = RefreshState.Refreshing
                    onRefreshClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
            )
            VerticalSpacer()
            ProfileUpgradeSection(
                isVisible = state.isUpgradeBannerVisible,
                contentPadding = PaddingValues(
                    horizontal = 64.dp,
                    vertical = verticalSpacing,
                ),
                onClick = onUpgradeProfileClick,
                onCloseClick = onCloseUpgradeProfileClick,
                modifier = Modifier
                    .background(MaterialTheme.colors.background)
                    .fillMaxWidth(),
            )
            MiniPlayerPadding(
                padding = state.miniPlayerPadding,
                isUpgradeBannerVisible = state.isUpgradeBannerVisible,
            )
        }
    }
}

internal data class ProfilePageState(
    val isSendReferralsEnabled: Boolean,
    val isPlaybackEnabled: Boolean,
    val isClaimReferralsEnabled: Boolean,
    val isUpgradeBannerVisible: Boolean,
    val miniPlayerPadding: Dp,
    val headerState: ProfileHeaderState,
    val statsState: ProfileStatsState,
    val referralsState: ReferralsViewModel.UiState,
    val refreshState: RefreshState?,
)

private val horizontalPadding = 16.dp
private val verticalSpacing = 16.dp

@Composable
private fun VerticalSpacer() {
    Spacer(
        modifier = Modifier.height(verticalSpacing),
    )
}

@Composable
private fun Toolbar(
    showReferralsIcon: Boolean,
    state: ReferralsViewModel.UiState,
    onSendReferralsClick: () -> Unit,
    onReferralsTooltipClick: () -> Unit,
    onReferralsTooltipShown: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.theme.colors.secondaryUi01)
            .padding(horizontal = horizontalPadding),
    ) {
        if (showReferralsIcon) {
            ReferralsIconWithTooltip(
                state = state,
                onIconClick = onSendReferralsClick,
                onTooltipClick = onReferralsTooltipClick,
                onTooltipShown = onReferralsTooltipShown,
            )
        }
        if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            Spacer(
                modifier = Modifier.weight(1f),
            )
        }
        IconButton(
            onClick = onSettingsClick,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_profile_settings),
                contentDescription = stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.settings),
                tint = MaterialTheme.theme.colors.secondaryIcon01,
            )
        }
    }
}

@Composable
private fun ColumnScope.HeaderWithStats(
    headerState: ProfileHeaderState,
    statsState: ProfileStatsState,
    onHeaderClick: () -> Unit,
) {
    if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) {
        ProfileHeader(
            state = headerState,
            onClick = onHeaderClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
        )
        VerticalSpacer()
        ProfileStats(
            state = statsState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
        )
        VerticalSpacer()
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
        ) {
            ProfileHeader(
                state = headerState,
                onClick = onHeaderClick,
                modifier = Modifier.weight(1f),
            )
            ProfileStats(
                state = statsState,
                modifier = Modifier.weight(1f),
            )
        }
        VerticalSpacer()
    }
}

@Composable
private fun MiniPlayerPadding(
    padding: Dp,
    isUpgradeBannerVisible: Boolean,
) {
    Box(
        modifier = Modifier
            .background(if (isUpgradeBannerVisible) MaterialTheme.colors.background else Color.Transparent)
            .fillMaxWidth()
            .height(padding),
    )
}

@OrientationPreview
@Composable
private fun ProfilePagePreview() {
    ProfilePageStub(Theme.ThemeType.ROSE)
}

@Preview
@Composable
private fun ProfilePageThemePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    ProfilePageStub(theme)
}

@Composable
private fun ProfilePageStub(
    theme: Theme.ThemeType,
) {
    ProfilePage(
        state = ProfilePageState(
            isSendReferralsEnabled = true,
            isPlaybackEnabled = true,
            isClaimReferralsEnabled = true,
            isUpgradeBannerVisible = true,
            miniPlayerPadding = 64.dp,
            headerState = ProfileHeaderState(
                email = "noreply@pocketcasts.com",
                imageUrl = null,
                subscriptionTier = SubscriptionTier.NONE,
                expiresIn = null,
            ),
            statsState = ProfileStatsState(
                podcastsCount = 50,
                listenedDuration = 75.hours,
                savedDuration = 35.minutes,
            ),
            referralsState = ReferralsViewModel.UiState.Loaded(
                showIcon = true,
                showTooltip = false,
                showProfileBanner = true,
                showHideBannerPopup = false,
                referralsOfferInfo = ReferralsOfferInfoMock,
            ),
            refreshState = RefreshState.Success(Date()),
        ),
        themeType = theme,
        onClaimReferralsClick = {},
        onReferralsTooltipClick = {},
        onReferralsTooltipShown = {},
        onSettingsClick = {},
        onHeaderClick = {},
        onPlaybackClick = {},
        onSendReferralsClick = {},
        onHideReferralsCardClick = {},
        onReferralsCardShown = {},
        onShowReferralsSheet = {},
        onSectionClick = {},
        onRefreshClick = {},
        onUpgradeProfileClick = {},
        onCloseUpgradeProfileClick = {},
    )
}
