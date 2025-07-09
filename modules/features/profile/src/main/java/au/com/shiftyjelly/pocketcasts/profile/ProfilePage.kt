package au.com.shiftyjelly.pocketcasts.profile

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AppBarDefaults
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
import au.com.shiftyjelly.pocketcasts.compose.PreviewOrientation
import au.com.shiftyjelly.pocketcasts.compose.components.Banner
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.endofyear.ui.EndOfYearPromptCard
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.flatMap
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.referrals.ReferralSubscriptionPlan
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsClaimGuestPassBannerCard
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsIconWithTooltip
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ProfilePage(
    state: ProfilePageState,
    themeType: Theme.ThemeType,
    onSendReferralsClick: () -> Unit,
    onReferralsTooltipClick: () -> Unit,
    onReferralsTooltipShow: () -> Unit,
    onSettingsClick: () -> Unit,
    onHeaderClick: () -> Unit,
    onCreateFreeAccountBannerClick: () -> Unit,
    onDismissCreateFreeAccountBannerClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onClaimReferralsClick: () -> Unit,
    onHideReferralsCardClick: () -> Unit,
    onReferralsCardShow: () -> Unit,
    onReferralsSheetShow: () -> Unit,
    onSectionClick: (ProfileSection) -> Unit,
    onRefreshClick: () -> Unit,
    onUpgradeProfileClick: () -> Unit,
    onCloseUpgradeProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val isPortrait = LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE
    AppTheme(themeType) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.theme.colors.primaryUi02),
        ) {
            Toolbar(
                state = state.referralsState,
                onSendReferralsClick = onSendReferralsClick,
                onReferralsTooltipClick = onReferralsTooltipClick,
                onReferralsTooltipShow = onReferralsTooltipShow,
                onSettingsClick = onSettingsClick,
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    VerticalSpacer()
                }
                headerWithStats(
                    headerState = state.headerState,
                    statsState = state.statsState,
                    onHeaderClick = onHeaderClick,
                    isPortrait = isPortrait,
                )
                item {
                    VerticalSpacer()
                }
                if (state.isFreeAccountBannerVisible) {
                    item {
                        Banner(
                            title = stringResource(LR.string.encourage_account_sync_banner_title),
                            description = stringResource(LR.string.encourage_account_sync_banner_description),
                            actionLabel = stringResource(LR.string.encourage_account_banner_action_label),
                            icon = painterResource(IR.drawable.ic_heart_2),
                            onActionClick = onCreateFreeAccountBannerClick,
                            onDismiss = onDismissCreateFreeAccountBannerClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPadding),
                        )
                    }
                    item {
                        VerticalSpacer()
                    }
                }
                if (state.isPlaybackEnabled) {
                    item {
                        EndOfYearPromptCard(
                            onClick = onPlaybackClick,
                            modifier = Modifier.padding(horizontal = horizontalPadding),
                        )
                    }
                    item {
                        VerticalSpacer()
                    }
                }
                item {
                    ReferralsClaimGuestPassBannerCard(
                        state = state.referralsState,
                        onClick = onClaimReferralsClick,
                        onHideBannerClick = onHideReferralsCardClick,
                        onBannerShow = onReferralsCardShow,
                        onShowReferralsSheet = onReferralsSheetShow,
                        modifier = Modifier.padding(horizontal = horizontalPadding),
                    )
                }
                if ((state.referralsState as? ReferralsViewModel.UiState.Loaded)?.showProfileBanner == true) {
                    item {
                        VerticalSpacer()
                    }
                }
                item {
                    HorizontalDivider()
                }
                item {
                    ProfileSections(
                        sections = ProfileSection.entries,
                        onClick = onSectionClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    VerticalSpacer()
                }
                item {
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
                }
                item {
                    VerticalSpacer()
                }
                item {
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
                }
                item {
                    MiniPlayerPadding(
                        padding = state.miniPlayerPadding,
                        isUpgradeBannerVisible = state.isUpgradeBannerVisible,
                    )
                }
            }
        }
    }
}

internal data class ProfilePageState(
    val isPlaybackEnabled: Boolean,
    val isFreeAccountBannerVisible: Boolean,
    val isUpgradeBannerVisible: Boolean,
    val miniPlayerPadding: Dp,
    val headerState: ProfileHeaderState,
    val statsState: ProfileStatsState,
    val referralsState: ReferralsViewModel.UiState,
    val refreshState: RefreshState,
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
    state: ReferralsViewModel.UiState,
    onSendReferralsClick: () -> Unit,
    onReferralsTooltipClick: () -> Unit,
    onReferralsTooltipShow: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.theme.colors.secondaryUi01)
            .windowInsetsPadding(AppBarDefaults.topAppBarWindowInsets)
            .height(56.dp)
            .padding(horizontal = horizontalPadding),
    ) {
        ReferralsIconWithTooltip(
            state = state,
            onIconClick = onSendReferralsClick,
            onTooltipClick = onReferralsTooltipClick,
            onTooltipShow = onReferralsTooltipShow,
        )
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

private fun LazyListScope.headerWithStats(
    headerState: ProfileHeaderState,
    statsState: ProfileStatsState,
    onHeaderClick: () -> Unit,
    isPortrait: Boolean,
) {
    if (isPortrait) {
        item {
            ProfileHeader(
                state = headerState,
                onClick = onHeaderClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
            )
        }
        item {
            VerticalSpacer()
        }
        item {
            ProfileStats(
                state = statsState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
            )
        }
        item {
            VerticalSpacer()
        }
    } else {
        item {
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
        }
        item {
            VerticalSpacer()
        }
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

@PreviewOrientation
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
            isPlaybackEnabled = true,
            isUpgradeBannerVisible = true,
            isFreeAccountBannerVisible = true,
            miniPlayerPadding = 64.dp,
            headerState = ProfileHeaderState(
                email = "noreply@pocketcasts.com",
                imageUrl = null,
                subscriptionTier = null,
                expiresIn = null,
            ),
            statsState = ProfileStatsState(
                podcastsCount = 50,
                listenedDuration = 75.hours,
                savedDuration = 35.minutes,
            ),
            referralsState = ReferralsViewModel.UiState.Loaded(
                referralPlan = SubscriptionPlans.Preview
                    .findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.Referral)
                    .flatMap(ReferralSubscriptionPlan::create)
                    .getOrNull()!!,
                showIcon = true,
                showTooltip = false,
                showProfileBanner = true,
                showHideBannerPopup = false,
            ),
            refreshState = RefreshState.Success(Date()),
        ),
        themeType = theme,
        onClaimReferralsClick = {},
        onReferralsTooltipClick = {},
        onReferralsTooltipShow = {},
        onSettingsClick = {},
        onHeaderClick = {},
        onCreateFreeAccountBannerClick = {},
        onDismissCreateFreeAccountBannerClick = {},
        onPlaybackClick = {},
        onSendReferralsClick = {},
        onHideReferralsCardClick = {},
        onReferralsCardShow = {},
        onReferralsSheetShow = {},
        onSectionClick = {},
        onRefreshClick = {},
        onUpgradeProfileClick = {},
        onCloseUpgradeProfileClick = {},
    )
}
