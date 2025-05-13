package au.com.shiftyjelly.pocketcasts.referrals

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.PricingPhase
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule.Period
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule.RecurrenceMode
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.utils.titlecaseFirstChar
import java.math.BigDecimal
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@ConsistentCopyVisibility
data class ReferralSubscriptionPlan private constructor(
    val key: SubscriptionPlan.Key,
    val freePricingPhase: PricingPhase,
    val paidPricingPhase: PricingPhase,
) {
    val offerName: String
        @Composable get() {
            val recurranceCount = (freePricingPhase.schedule.recurrenceMode as RecurrenceMode.Recurring).value
            val periodCount = freePricingPhase.schedule.periodCount
            val totalCount = recurranceCount * periodCount

            val periodText = when (freePricingPhase.schedule.period) {
                Period.Daily -> stringResource(LR.string.plus_day)
                Period.Weekly -> stringResource(LR.string.plus_week)
                Period.Monthly -> stringResource(LR.string.plus_month)
                Period.Yearly -> stringResource(LR.string.plus_year)
            }.titlecaseFirstChar()
            return "$totalCount-$periodText"
        }

    val offerDurationText: String
        @Composable get() {
            val recurranceCount = (freePricingPhase.schedule.recurrenceMode as RecurrenceMode.Recurring).value
            val periodCount = freePricingPhase.schedule.periodCount
            val totalCount = recurranceCount * periodCount

            return when (freePricingPhase.schedule.period) {
                Period.Daily -> pluralStringResource(LR.plurals.day_with_count, totalCount, totalCount)
                Period.Weekly -> pluralStringResource(LR.plurals.week_with_count, totalCount, totalCount)
                Period.Monthly -> pluralStringResource(LR.plurals.month_with_count, totalCount, totalCount)
                Period.Yearly -> pluralStringResource(LR.plurals.year_with_count, totalCount, totalCount)
            }
        }

    val priceAfterOffer get() = paidPricingPhase.price

    companion object {
        fun create(plan: SubscriptionPlan.WithOffer): PaymentResult<ReferralSubscriptionPlan> {
            return when {
                plan.offer != SubscriptionOffer.Referral -> PaymentResult.Failure(
                    PaymentResultCode.DeveloperError,
                    "Can't create referral offer from ${plan.offer}",
                )
                plan.pricingPhases.size != 2 -> PaymentResult.Failure(
                    PaymentResultCode.DeveloperError,
                    "${plan.offer} should have 2 pricing phases",
                )
                plan.pricingPhases[0].price.amount.stripTrailingZeros() != BigDecimal.ZERO -> PaymentResult.Failure(
                    PaymentResultCode.DeveloperError,
                    "${plan.offer} should have free initial period. Found ${plan.pricingPhases[0].price}",
                )
                plan.pricingPhases[0].schedule.recurrenceMode !is RecurrenceMode.Recurring -> PaymentResult.Failure(
                    PaymentResultCode.DeveloperError,
                    "${plan.offer} should have recurring initial period",
                )
                plan.pricingPhases[1].schedule.recurrenceMode != RecurrenceMode.Infinite -> PaymentResult.Failure(
                    PaymentResultCode.DeveloperError,
                    "${plan.offer} should have infinite second period",
                )
                else -> PaymentResult.Success(
                    ReferralSubscriptionPlan(plan.key, plan.pricingPhases[0], plan.pricingPhases[1]),
                )
            }
        }
    }
}
