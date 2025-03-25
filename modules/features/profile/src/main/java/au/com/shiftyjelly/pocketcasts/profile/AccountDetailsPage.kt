package au.com.shiftyjelly.pocketcasts.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.FeatureCardsState
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.ProfileUpgradeBanner
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.UpgradeFeatureCard
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AutomotivePreview
import au.com.shiftyjelly.pocketcasts.compose.OrientationPreview
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.UserAvatarConfig
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.profile.winback.WinbackInitParams
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.time.Duration.Companion.days
import au.com.shiftyjelly.pocketcasts.account.viewmodel.ProfileUpgradeBannerViewModel.State as UpgradeBannerState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AccountDetailsPage(
    state: AccountDetailsPageState,
    theme: Theme.ThemeType,
    onNavigateBack: () -> Unit,
    onClickHeader: () -> Unit,
    onClickUpgradeBanner: () -> Unit,
    onFeatureCardChanged: (UpgradeFeatureCard) -> Unit,
    onChangeAvatar: (String) -> Unit,
    onChangeEmail: () -> Unit,
    onChangePassword: () -> Unit,
    onUpgradeToPatron: () -> Unit,
    onCancelSubscription: (WinbackInitParams) -> Unit,
    onChangeNewsletterSubscription: (Boolean) -> Unit,
    onShowPrivacyPolicy: () -> Unit,
    onShowTermsOfUse: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val headerConfig = remember(state.isAutomotive) {
        if (state.isAutomotive) {
            AccountHeaderConfig(
                avatarConfig = UserAvatarConfig(
                    imageSize = 104.dp,
                    strokeWidth = 4.dp,
                    imageContentPadding = 4.dp,
                    badgeFontSize = 18.sp,
                    badgeIconSize = 18.dp,
                    badgeContentPadding = 6.dp,
                ),
                infoFontScale = 1.6f,
            )
        } else {
            AccountHeaderConfig(
                avatarConfig = UserAvatarConfig(
                    imageSize = 64.dp,
                    strokeWidth = 3.dp,
                ),
            )
        }
    }
    val sectionsConfig = remember(state.isAutomotive) {
        if (state.isAutomotive) {
            AccountSectionsConfig(
                iconSize = 48.dp,
                fontScale = 2f,
            )
        } else {
            AccountSectionsConfig()
        }
    }
    val sectionsState = remember(state.isAutomotive, state.sectionsState) {
        if (state.isAutomotive) {
            state.sectionsState.copy(
                availableSections = listOf(AccountSection.SignOut),
            )
        } else {
            state.sectionsState
        }
    }
    val bannerState = remember(state.isAutomotive, state.upgradeBannerState) {
        state.upgradeBannerState.takeIf { !state.isAutomotive }
    }
    AppTheme(theme) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.theme.colors.primaryUi02),
        ) {
            if (!state.isAutomotive) {
                ThemedTopAppBar(
                    title = stringResource(LR.string.profile_pocket_casts_account),
                    onNavigationClick = onNavigateBack,
                )
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = modifier.fillMaxSize(),
            ) {
                item {
                    Spacer(Modifier.height(16.dp))
                }
                item {
                    AccountHeader(
                        state = state.headerState,
                        config = headerConfig,
                        onClick = onClickHeader,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .then(if (state.isAutomotive) Modifier.padding(top = 32.dp) else Modifier),
                    )
                }
                if (bannerState is UpgradeBannerState.Loaded) {
                    item {
                        HorizontalDivider()
                    }
                    item {
                        ProfileUpgradeBanner(
                            state = bannerState,
                            onClick = onClickUpgradeBanner,
                            onFeatureCardChanged = onFeatureCardChanged,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item {
                    AccountSections(
                        state = sectionsState,
                        config = sectionsConfig,
                        onChangeAvatar = onChangeAvatar,
                        onChangeEmail = onChangeEmail,
                        onChangePassword = onChangePassword,
                        onUpgradeToPatron = onUpgradeToPatron,
                        onCancelSubscription = onCancelSubscription,
                        onChangeNewsletterSubscription = onChangeNewsletterSubscription,
                        onShowPrivacyPolicy = onShowPrivacyPolicy,
                        onShowTermsOfUse = onShowTermsOfUse,
                        onSignOut = onSignOut,
                        onDeleteAccount = onDeleteAccount,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Spacer(Modifier.height(state.miniPlayerPadding))
                }
            }
        }
    }
}

internal data class AccountDetailsPageState(
    val isAutomotive: Boolean,
    val miniPlayerPadding: Dp,
    val headerState: AccountHeaderState,
    val upgradeBannerState: UpgradeBannerState?,
    val sectionsState: AccountSectionsState,
)

@OrientationPreview
@Composable
private fun AccountDetailsPagePreview() {
    AccountDetailsPageStub(Theme.ThemeType.ELECTRIC)
}

@Preview
@Composable
private fun AccountDetailsPageNoUpgradePreview() {
    AccountDetailsPageStub(Theme.ThemeType.ELECTRIC, upgradeBannerState = null)
}

@AutomotivePreview
@Composable
private fun AccountDetailsPageAutomotivePreview() {
    AccountDetailsPageStub(Theme.ThemeType.DARK, isAutomotive = true)
}

@Preview
@Composable
private fun AccountDetailsPageThemePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AccountDetailsPageStub(theme)
}

@Composable
private fun AccountDetailsPageStub(
    theme: Theme.ThemeType,
    isAutomotive: Boolean = false,
    upgradeBannerState: UpgradeBannerState? = UpgradeBannerState.Loaded(
        featureCardsState = FeatureCardsState(
            subscriptions = emptyList(),
            currentFeatureCard = UpgradeFeatureCard.PLUS,
            currentFrequency = SubscriptionFrequency.NONE,
        ),
        upgradeButtons = emptyList(),
    ),
) {
    AccountDetailsPage(
        state = AccountDetailsPageState(
            isAutomotive = isAutomotive,
            miniPlayerPadding = 64.dp,
            headerState = AccountHeaderState(
                email = "noreplay@pocketcasts.com",
                imageUrl = null,
                subscription = SubscriptionHeaderState.PaidRenew(
                    tier = SubscriptionTier.PATRON,
                    expiresIn = 36.days,
                    frequency = SubscriptionFrequency.MONTHLY,
                ),
            ),
            upgradeBannerState = upgradeBannerState,
            sectionsState = AccountSectionsState(
                isSubscribedToNewsLetter = false,
                email = "noreplay@pocketcasts.com",
                winbackInitParams = WinbackInitParams.Empty,
                canChangeCredentials = true,
                canUpgradeAccount = true,
                canCancelSubscription = true,
            ),
        ),
        theme = theme,
        onNavigateBack = {},
        onClickHeader = {},
        onClickUpgradeBanner = {},
        onFeatureCardChanged = {},
        onChangeAvatar = {},
        onChangeEmail = {},
        onChangePassword = {},
        onUpgradeToPatron = {},
        onCancelSubscription = {},
        onChangeNewsletterSubscription = {},
        onShowPrivacyPolicy = {},
        onShowTermsOfUse = {},
        onSignOut = {},
        onDeleteAccount = {},
    )
}
