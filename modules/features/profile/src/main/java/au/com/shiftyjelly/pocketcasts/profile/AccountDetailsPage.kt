package au.com.shiftyjelly.pocketcasts.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingSubscriptionPlan
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeHelper
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.ProfileUpgradeBanner
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.ProfileUpgradeBannerState
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.PreviewAutomotive
import au.com.shiftyjelly.pocketcasts.compose.PreviewOrientation
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.components.UserAvatarConfig
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadge
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.profile.winback.WinbackInitParams
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.time.Duration.Companion.days
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AccountDetailsPage(
    state: AccountDetailsPageState,
    theme: Theme.ThemeType,
    onNavigateBack: () -> Unit,
    onClickHeader: () -> Unit,
    onClickSubscribe: (SubscriptionPlan.Key) -> Unit,
    onChangeFeatureCard: (SubscriptionPlan.Key) -> Unit,
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
    onUpgradeClick: () -> Unit,
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
                modifier = Modifier.fillMaxSize(),
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
                when (bannerState) {
                    is ProfileUpgradeBannerState.OldProfileUpgradeBannerState -> {
                        item {
                            Divider()
                        }
                        item {
                            ProfileUpgradeBanner(
                                state = bannerState,
                                onClickSubscribe = onClickSubscribe,
                                onChangeFeatureCard = onChangeFeatureCard,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    is ProfileUpgradeBannerState.NewOnboardingUpgradeState -> {
                        item {
                            Box(modifier = Modifier.padding(24.dp)) {
                                NewUpgradeAccountCard(
                                    onClickSubscribe = onUpgradeClick,
                                    recommendedPlan = bannerState.recommendedSubscription,
                                )
                            }
                        }
                    }

                    else -> Unit
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

@Composable
private fun NewUpgradeAccountCard(
    recommendedPlan: OnboardingSubscriptionPlan,
    onClickSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SubscriptionBadge(
                iconRes = recommendedPlan.badgeIconRes,
                shortNameRes = recommendedPlan.shortNameRes,
                backgroundColor = Color.Black,
                textColor = Color.White,
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
            )
            TextP30(
                text = stringResource(recommendedPlan.pageTitle),
                color = MaterialTheme.theme.colors.primaryText01,
                fontWeight = FontWeight.W700,
            )
            TextP60(
                text = stringResource(LR.string.onboarding_upgrade_account_message),
                color = MaterialTheme.theme.colors.primaryText02,
                textAlign = TextAlign.Center,
            )
            OnboardingUpgradeHelper.UpgradeRowButton(
                primaryText = recommendedPlan.ctaButtonText(false),
                backgroundColor = recommendedPlan.ctaButtonBackgroundColor,
                fontWeight = FontWeight.W500,
                textColor = recommendedPlan.ctaButtonTextColor,
                onClick = onClickSubscribe,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            )
        }
    }
}

internal data class AccountDetailsPageState(
    val isAutomotive: Boolean,
    val miniPlayerPadding: Dp,
    val headerState: AccountHeaderState,
    val upgradeBannerState: ProfileUpgradeBannerState?,
    val sectionsState: AccountSectionsState,
)

@PreviewOrientation
@Composable
private fun AccountDetailsPagePreview() {
    AccountDetailsPageStub(Theme.ThemeType.ELECTRIC)
}

@Preview
@Composable
private fun AccountDetailsPageNoUpgradePreview() {
    AccountDetailsPageStub(Theme.ThemeType.ELECTRIC, upgradeBannerState = null)
}

@PreviewAutomotive
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

@Preview
@Composable
private fun AccountDetailsPageNewUpgradePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AccountDetailsPageStub(
        theme = theme,
        upgradeBannerState = ProfileUpgradeBannerState.NewOnboardingUpgradeState(
            recommendedSubscription = OnboardingSubscriptionPlan.create(SubscriptionPlan.PlusMonthlyPreview),
        ),
    )
}

@Composable
private fun AccountDetailsPageStub(
    theme: Theme.ThemeType,
    isAutomotive: Boolean = false,
    upgradeBannerState: ProfileUpgradeBannerState? = ProfileUpgradeBannerState.OldProfileUpgradeBannerState(
        subscriptionPlans = SubscriptionPlans.Preview,
        selectedFeatureCard = null,
        currentSubscription = null,
        isRenewingSubscription = false,
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
                    tier = SubscriptionTier.Patron,
                    expiresIn = 36.days,
                    billingCycle = BillingCycle.Monthly,
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
        onClickSubscribe = {},
        onChangeFeatureCard = {},
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
        onUpgradeClick = {},
    )
}
