package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun AvailablePlanRow(
    plan: SubscriptionPlan.Base,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SubscriptionPlanRow(
        plan = plan,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
        badgeConfig = BadgeConfig.availablePlansConfig(),
    )
}

@Composable
fun UpgradePlanRow(
    plan: SubscriptionPlan.Base,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    otherPlan: SubscriptionPlan.Base? = null,
) {
    SubscriptionPlanRow(
        plan = plan,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
        badgeConfig = BadgeConfig.upgradePlansConfig(calculatedSavingPercent = otherPlan?.let { plan.savingsPercent(otherPlan) }),
    )
}

private enum class BadgePosition {
    RIGHT,
    CENTER,
}

private sealed interface BadgeText {

    @Composable
    fun text(): String

    data class StaticTextResource(
        @StringRes val stringResId: Int,
    ) : BadgeText {

        @Composable
        override fun text() = stringResource(stringResId)
    }

    data class CalculatedText(
        val provider: @Composable () -> String,
    ) : BadgeText {
        @Composable
        override fun text() = provider()
    }
}

private data class BadgeConfig(
    val badgeText: BadgeText,
    val badgePosition: BadgePosition,
) {
    companion object {
        fun availablePlansConfig() = BadgeConfig(
            badgeText = BadgeText.StaticTextResource(LR.string.best_value),
            badgePosition = BadgePosition.RIGHT,
        )

        fun upgradePlansConfig(calculatedSavingPercent: Int?) = BadgeConfig(
            badgeText = BadgeText.CalculatedText { calculatedSavingPercent?.let { stringResource(LR.string.onboarding_upgrade_save_percent, calculatedSavingPercent) }.orEmpty() },
            badgePosition = BadgePosition.CENTER,
        )
    }
}

@Composable
private fun SubscriptionPlanRow(
    plan: SubscriptionPlan.Base,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeConfig: BadgeConfig,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.theme.colors.primaryUi01Active)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.theme.colors.primaryField03Active,
                            shape = RoundedCornerShape(8.dp),
                        )
                    } else {
                        Modifier
                    },
                )
                .clickable(
                    enabled = !isSelected,
                    onClick = onClick,
                    role = Role.Button,
                    indication = ripple(color = MaterialTheme.theme.colors.primaryIcon01),
                    interactionSource = null,
                )
                .padding(16.dp),
        ) {
            CheckMark(
                isSelected = isSelected,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(22.dp),
            )
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
            ) {
                TextH30(
                    text = plan.name,
                )
                TextP40(
                    text = plan.price(),
                    color = MaterialTheme.theme.colors.primaryText02,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                )
            }
            if (plan.billingCycle == BillingCycle.Yearly) {
                val currencyCode = plan.pricingPhase.price.currencyCode
                TextP40(
                    text = if (currencyCode == "USD") {
                        stringResource(LR.string.price_per_month_usd, plan.pricePerMonth)
                    } else {
                        stringResource(LR.string.price_per_month, plan.pricePerMonth, currencyCode)
                    },
                    color = MaterialTheme.theme.colors.primaryText02,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }

        if (plan.tier == SubscriptionTier.Plus && plan.billingCycle == BillingCycle.Yearly) {
            Box(
                modifier = Modifier
                    .align(
                        when (badgeConfig.badgePosition) {
                            BadgePosition.RIGHT -> Alignment.TopEnd
                            BadgePosition.CENTER -> Alignment.TopCenter
                        },
                    )
                    .offset(x = (-10).dp, y = (-10).dp)
                    .background(MaterialTheme.theme.colors.primaryField03Active, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            ) {
                TextH50(
                    text = badgeConfig.badgeText.text(),
                    color = MaterialTheme.theme.colors.primaryUi01,
                    disableAutoScale = true,
                )
            }
        }
    }
}

@Composable
private fun CheckMark(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isSelected) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.background(
                color = MaterialTheme.theme.colors.primaryField03Active,
                shape = CircleShape,
            ),
        ) {
            Image(
                painter = painterResource(IR.drawable.ic_checkmark_small),
                colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryUi01),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
            )
        }
    } else {
        Box(
            modifier = modifier.border(
                width = 1.dp,
                color = MaterialTheme.theme.colors.primaryIcon02,
                shape = CircleShape,
            ),
        )
    }
}

private val SubscriptionPlan.Base.pricePerMonth: Float
    get() {
        val pricePerWeek = when (billingCycle) {
            BillingCycle.Monthly -> pricingPhase.price.amount
            BillingCycle.Yearly -> pricingPhase.price.amount / 12.toBigDecimal()
        }
        return pricePerWeek.toFloat()
    }

private fun SubscriptionPlan.Base.savingsPercent(otherPlan: SubscriptionPlan.Base): Int {
    if (this.billingCycle != BillingCycle.Yearly || otherPlan.billingCycle != BillingCycle.Monthly) return 0
    return 100 - ((this.pricePerMonth / otherPlan.pricePerMonth) * 100).toInt()
}

@Composable
@ReadOnlyComposable
private fun SubscriptionPlan.Base.price() = when (billingCycle) {
    BillingCycle.Monthly -> stringResource(LR.string.plus_per_month, pricingPhase.price.formattedPrice)
    BillingCycle.Yearly -> stringResource(LR.string.plus_per_year, pricingPhase.price.formattedPrice)
}

@Preview
@Composable
private fun PreviewAvailablePlanSelectors(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AvailablePlanRow(
                plan = SubscriptionPlan.PlusYearlyPreview,
                isSelected = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
            )
            AvailablePlanRow(
                plan = SubscriptionPlan.PlusMonthlyPreview,
                isSelected = false,
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewUpgradePlanSelectors(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UpgradePlanRow(
                plan = SubscriptionPlan.PlusYearlyPreview,
                isSelected = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
                otherPlan = SubscriptionPlan.PlusMonthlyPreview,
            )
            UpgradePlanRow(
                plan = SubscriptionPlan.PlusMonthlyPreview,
                isSelected = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
            )
        }
    }
}
