package au.com.shiftyjelly.pocketcasts.models.type

import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.localization.R
import com.android.billingclient.api.ProductDetails

sealed interface Subscription {
    val tier: SubscriptionTier
    val recurringPricingPhase: RecurringSubscriptionPricingPhase
    val trialPricingPhase: TrialSubscriptionPricingPhase?
    val productDetails: ProductDetails
    val offerToken: String
    val shortTitle: String
        get() = productDetails.title.split(" (").first()

    fun numFreeThenPricePerPeriod(res: Resources): String?
    fun tryFreeThenPricePerPeriod(res: Resources): String?

    // Simple subscriptions do not have a trial phase
    class Simple(
        override val tier: SubscriptionTier,
        override val recurringPricingPhase: RecurringSubscriptionPricingPhase,
        override val productDetails: ProductDetails,
        override val offerToken: String
    ) : Subscription {
        override val trialPricingPhase = null
        override fun numFreeThenPricePerPeriod(res: Resources): String? = null
        override fun tryFreeThenPricePerPeriod(res: Resources): String? = null
    }

    class WithTrial(
        override val tier: SubscriptionTier,
        override val recurringPricingPhase: RecurringSubscriptionPricingPhase,
        override val trialPricingPhase: TrialSubscriptionPricingPhase, // override to not be nullable
        override val productDetails: ProductDetails,
        override val offerToken: String
    ) : Subscription {
        override fun numFreeThenPricePerPeriod(res: Resources): String {
            val stringRes = when (recurringPricingPhase) {
                is SubscriptionPricingPhase.Years -> R.string.plus_trial_then_slash_year
                is SubscriptionPricingPhase.Months -> R.string.plus_trial_then_slash_month
            }
            return res.getString(
                stringRes,
                trialPricingPhase.periodValuePlural(res),
                recurringPricingPhase.formattedPrice
            )
        }
        override fun tryFreeThenPricePerPeriod(res: Resources): String {
            val stringRes = when (recurringPricingPhase) {
                is SubscriptionPricingPhase.Years -> R.string.trial_then_per_year
                is SubscriptionPricingPhase.Months -> R.string.trial_then_per_month
            }
            return res.getString(
                stringRes,
                trialPricingPhase.periodValuePlural(res),
                recurringPricingPhase.formattedPrice
            )
        }
    }

    enum class SubscriptionTier { PLUS, PATRON, UNKNOWN }

    companion object {
        const val PLUS_PRODUCT_BASE = "com.pocketcasts.plus"
        const val PLUS_MONTHLY_PRODUCT_ID = "$PLUS_PRODUCT_BASE.monthly"
        const val PLUS_YEARLY_PRODUCT_ID = "$PLUS_PRODUCT_BASE.yearly"
        const val PATRON_MONTHLY_PRODUCT_ID = "com.pocketcasts.monthly.patron"
        const val PATRON_YEARLY_PRODUCT_ID = "com.pocketcasts.yearly.patron"

        fun fromProductDetails(productDetails: ProductDetails, isFreeTrialEligible: Boolean): Subscription? =
            SubscriptionMapper.map(productDetails, isFreeTrialEligible)
    }
}
