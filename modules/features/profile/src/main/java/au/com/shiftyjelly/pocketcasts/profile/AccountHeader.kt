package au.com.shiftyjelly.pocketcasts.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.components.UserAvatar
import au.com.shiftyjelly.pocketcasts.compose.components.UserAvatarConfig
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeDisplayMode
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeForTier
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralDaysMonthsOrYears
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatLongStyle
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AccountHeader(
    state: AccountHeaderState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: AccountHeaderConfig = AccountHeaderConfig(),
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(
            onClick = onClick,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            UserAvatar(
                imageUrl = state.imageUrl,
                subscriptionTier = state.subscription.tier,
                borderCompletion = state.subscription.expiresIn?.let { it / 30.days }?.toFloat() ?: 1f,
                config = config.avatarConfig,
                showBadge = false,
            )
            if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
                TextH70(
                    text = state.email,
                    fontScale = config.infoFontScale,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.theme.colors.primaryText01,
                )
            } else {
                TextH50(
                    text = state.email,
                    fontScale = config.infoFontScale,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.theme.colors.primaryText01,
                )
            }
        }
        val tier = state.subscription.tier
        if (tier != null) {
            Spacer(
                modifier = Modifier.height(
                    if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
                        8.dp
                    } else {
                        16.dp
                    },
                ),
            )
            SubscriptionBadgeForTier(
                tier = tier,
                displayMode = when (tier) {
                    SubscriptionTier.Plus -> SubscriptionBadgeDisplayMode.Black
                    SubscriptionTier.Patron -> SubscriptionBadgeDisplayMode.Colored
                },
                iconSize = config.avatarConfig.badgeIconSize,
                fontSize = config.avatarConfig.badgeFontSize,
                padding = config.avatarConfig.badgeContentPadding,
            )
        }
        Spacer(
            modifier = Modifier.height(
                if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
                    8.dp
                } else {
                    16.dp
                },
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            val labels = state.subscription.labels()
            if (labels.start != null) {
                if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE) && labels.end == null) {
                    TextH70(
                        text = labels.start.text,
                        fontScale = config.infoFontScale,
                        color = MaterialTheme.theme.colors.primaryText01,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    TextH50(
                        text = labels.start.text,
                        fontScale = config.infoFontScale,
                        color = labels.start.color(MaterialTheme.theme.colors),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (labels.end != null) {
                Spacer(
                    modifier = Modifier.width(16.dp),
                )
                TextH50(
                    text = labels.end.text,
                    fontScale = config.infoFontScale,
                    color = labels.end.color(MaterialTheme.theme.colors),
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

internal data class AccountHeaderConfig(
    val infoFontScale: Float = 1f,
    val avatarConfig: UserAvatarConfig = UserAvatarConfig(),
)

internal data class AccountHeaderState(
    val email: String,
    val imageUrl: String?,
    val subscription: SubscriptionHeaderState,
) {
    companion object {
        fun empty() = AccountHeaderState(
            email = "",
            imageUrl = null,
            subscription = SubscriptionHeaderState.Free,
        )
    }
}

internal sealed interface SubscriptionHeaderState {
    val tier: SubscriptionTier?
    val expiresIn: Duration?
    val isChampion: Boolean

    data object Free : SubscriptionHeaderState {
        override val tier = null
        override val expiresIn = null
        override val isChampion = false
    }

    data class PaidRenew(
        override val tier: SubscriptionTier,
        override val expiresIn: Duration,
        val billingCycle: BillingCycle?,
    ) : SubscriptionHeaderState {
        override val isChampion = false
    }

    data class PaidCancel(
        override val tier: SubscriptionTier,
        override val expiresIn: Duration,
        override val isChampion: Boolean,
        val platform: SubscriptionPlatform,
        val giftDaysLeft: Int,
    ) : SubscriptionHeaderState
}

@Composable
private fun SubscriptionHeaderState.labels(): Labels {
    val context = LocalContext.current
    return remember(this) {
        if (isChampion) {
            Labels(
                start = Label(
                    text = context.getString(LR.string.plus_thanks_for_your_support_bang),
                ),
                end = Label(
                    text = context.getString(LR.string.pocket_casts_champion),
                    color = { support02 },
                ),
            )
        } else {
            when (this) {
                is SubscriptionHeaderState.Free -> {
                    Labels(
                        start = Label(
                            text = context.getString(LR.string.profile_free_account),
                        ),
                    )
                }

                is SubscriptionHeaderState.PaidRenew -> {
                    val expiryDate = Date(Date().time + expiresIn.inWholeMilliseconds)
                    Labels(
                        start = Label(
                            text = context.getString(LR.string.profile_next_payment, expiryDate.toLocalizedFormatLongStyle()),
                        ),
                        end = Label(
                            text = when (billingCycle) {
                                BillingCycle.Monthly -> context.getString(LR.string.profile_monthly)
                                BillingCycle.Yearly -> context.getString(LR.string.profile_yearly)
                                null -> ""
                            },
                        ),
                    )
                }

                is SubscriptionHeaderState.PaidCancel -> {
                    Labels(
                        start = Label(
                            text = when {
                                platform == SubscriptionPlatform.Gift -> {
                                    val daysString = context.resources.getStringPluralDaysMonthsOrYears(giftDaysLeft)
                                    context.getString(LR.string.profile_time_free, daysString)
                                }

                                else -> {
                                    context.getString(LR.string.profile_payment_cancelled)
                                }
                            },
                        ),
                        end = Label(
                            text = run {
                                val expiryDate = Date(Date().time + expiresIn.inWholeMilliseconds)
                                context.getString(LR.string.profile_plus_expires, expiryDate.toLocalizedFormatLongStyle())
                            },
                        ),
                    )
                }
            }
        }
    }
}

private data class Labels(
    val start: Label? = null,
    val end: Label? = null,
)

private data class Label(
    val text: String,
    val color: @Composable ThemeColors.() -> Color = { primaryText02 },
)

@Preview
@Composable
private fun AccountHeaderPreview(
    @PreviewParameter(AccountHeaderStateParameterProvider::class) state: AccountHeaderState,
) {
    AppTheme(Theme.ThemeType.LIGHT) {
        Box(
            modifier = Modifier.background(MaterialTheme.theme.colors.primaryUi02),
        ) {
            AccountHeader(
                state = state,
                config = AccountHeaderConfig(
                    avatarConfig = UserAvatarConfig(
                        imageSize = 66.dp,
                        strokeWidth = 3.dp,
                    ),
                ),
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Preview(fontScale = 2f)
@Composable
private fun AccountHeaderFontSizePreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        Box(
            modifier = Modifier.background(MaterialTheme.theme.colors.primaryUi02),
        ) {
            AccountHeader(
                state = AccountHeaderState(
                    email = "noreply@pocketcasts.com",
                    imageUrl = null,
                    subscription = SubscriptionHeaderState.PaidCancel(
                        tier = SubscriptionTier.Plus,
                        expiresIn = 10.days,
                        isChampion = true,
                        platform = SubscriptionPlatform.Android,
                        giftDaysLeft = 0,
                    ),
                ),
                config = AccountHeaderConfig(
                    avatarConfig = UserAvatarConfig(
                        imageSize = 66.dp,
                        strokeWidth = 3.dp,
                    ),
                ),
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

private class AccountHeaderStateParameterProvider : PreviewParameterProvider<AccountHeaderState> {
    override val values = sequenceOf(
        AccountHeaderState(
            email = "noreply@pocketcasts.com",
            imageUrl = null,
            subscription = SubscriptionHeaderState.Free,
        ),
        AccountHeaderState(
            email = "noreply@pocketcasts.com",
            imageUrl = null,
            subscription = SubscriptionHeaderState.PaidRenew(
                tier = SubscriptionTier.Patron,
                expiresIn = 30.days,
                billingCycle = BillingCycle.Monthly,
            ),
        ),
        AccountHeaderState(
            email = "noreply@pocketcasts.com",
            imageUrl = null,
            subscription = SubscriptionHeaderState.PaidRenew(
                tier = SubscriptionTier.Plus,
                expiresIn = 30.days,
                billingCycle = BillingCycle.Monthly,
            ),
        ),
        AccountHeaderState(
            email = "noreply@pocketcasts.com",
            imageUrl = null,
            subscription = SubscriptionHeaderState.PaidRenew(
                tier = SubscriptionTier.Plus,
                expiresIn = 15.days,
                billingCycle = BillingCycle.Yearly,
            ),
        ),
        AccountHeaderState(
            email = "noreply@pocketcasts.com",
            imageUrl = null,
            subscription = SubscriptionHeaderState.PaidRenew(
                tier = SubscriptionTier.Plus,
                expiresIn = 8.days,
                billingCycle = null,
            ),
        ),
        AccountHeaderState(
            email = "noreply@pocketcasts.com",
            imageUrl = null,
            subscription = SubscriptionHeaderState.PaidCancel(
                tier = SubscriptionTier.Plus,
                expiresIn = 10.days,
                isChampion = false,
                platform = SubscriptionPlatform.Android,
                giftDaysLeft = 0,
            ),
        ),
        AccountHeaderState(
            email = "noreply@pocketcasts.com",
            imageUrl = null,
            subscription = SubscriptionHeaderState.PaidCancel(
                tier = SubscriptionTier.Plus,
                expiresIn = 10.days,
                isChampion = true,
                platform = SubscriptionPlatform.Android,
                giftDaysLeft = 0,
            ),
        ),
        AccountHeaderState(
            email = "noreply@pocketcasts.com",
            imageUrl = null,
            subscription = SubscriptionHeaderState.PaidCancel(
                tier = SubscriptionTier.Plus,
                expiresIn = 10.days,
                isChampion = false,
                platform = SubscriptionPlatform.Gift,
                giftDaysLeft = 5,
            ),
        ),
        AccountHeaderState(
            email = "noreply@pocketcasts.com",
            imageUrl = null,
            subscription = SubscriptionHeaderState.PaidCancel(
                tier = SubscriptionTier.Plus,
                expiresIn = 10.days,
                isChampion = false,
                platform = SubscriptionPlatform.Android,
                giftDaysLeft = 5,
            ),
        ),
    )
}
