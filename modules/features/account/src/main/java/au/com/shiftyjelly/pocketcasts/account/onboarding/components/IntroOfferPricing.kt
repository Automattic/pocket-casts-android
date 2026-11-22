package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import au.com.shiftyjelly.pocketcasts.payment.Price
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule
import au.com.shiftyjelly.pocketcasts.payment.PricingSchedule.RecurrenceMode
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import java.math.BigDecimal
import java.math.RoundingMode

internal data class IntroOfferPricing(
    val introPrice: Price,
    val regularPrice: Price,
) {
    val discountPercent: Int
        get() {
            if (regularPrice.amount.signum() <= 0) return 0
            val introFraction = introPrice.amount.divide(regularPrice.amount, 4, RoundingMode.HALF_UP)
            return BigDecimal.ONE.minus(introFraction).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toInt()
        }
}

// The paywall copy and renewal date assume one discounted year, then the regular price forever
internal val SubscriptionPlan.introOfferPricing: IntroOfferPricing?
    get() {
        if (this !is SubscriptionPlan.WithOffer || offer != SubscriptionOffer.IntroOffer || isInstallment) return null
        val (introPhase, regularPhase) = pricingPhases.takeIf { it.size == 2 } ?: return null
        if (introPhase.schedule != OneYearSchedule) return null
        if (regularPhase.schedule.recurrenceMode != RecurrenceMode.Infinite) return null
        return IntroOfferPricing(introPrice = introPhase.price, regularPrice = regularPhase.price)
    }

internal val List<SubscriptionPlan>.introOfferPricing: IntroOfferPricing?
    get() = firstNotNullOfOrNull { plan -> plan.introOfferPricing }

private val OneYearSchedule = PricingSchedule(RecurrenceMode.Recurring(1), PricingSchedule.Period.Yearly, periodCount = 1)
