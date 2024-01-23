package au.com.shiftyjelly.pocketcasts.models.type

import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import com.android.billingclient.api.ProductDetails

sealed interface Subscription {
    val tier: SubscriptionTier
    val recurringPricingPhase: RecurringSubscriptionPricingPhase
    val offerPricingPhase: OfferSubscriptionPricingPhase?
    val productDetails: ProductDetails
    val offerToken: String
    val shortTitle: String
        get() = productDetails.title.split(" (").first()

    fun numFreeThenPricePerPeriod(res: Resources): String?
    fun tryFreeThenPricePerPeriod(res: Resources): String?

    // Simple subscriptions do not have a offer phase
    class Simple(
        override val tier: SubscriptionTier,
        override val recurringPricingPhase: RecurringSubscriptionPricingPhase,
        override val productDetails: ProductDetails,
        override val offerToken: String,
    ) : Subscription {
        override val offerPricingPhase = null
        override fun numFreeThenPricePerPeriod(res: Resources): String? = null
        override fun tryFreeThenPricePerPeriod(res: Resources): String? = null
    }

    sealed class WithOffer(
        override val tier: SubscriptionTier,
        override val recurringPricingPhase: RecurringSubscriptionPricingPhase,
        override val offerPricingPhase: OfferSubscriptionPricingPhase, // override to not be nullable
        override val productDetails: ProductDetails,
        override val offerToken: String,
    ) : Subscription {
        abstract fun badgeOfferText(): Int
    }

    class Trial(
        tier: SubscriptionTier,
        recurringPricingPhase: RecurringSubscriptionPricingPhase,
        offerPricingPhase: OfferSubscriptionPricingPhase,
        productDetails: ProductDetails,
        offerToken: String,
    ) : WithOffer(tier, recurringPricingPhase, offerPricingPhase, productDetails, offerToken) {
        override fun badgeOfferText(): Int = R.string.one_month_free_trial_badge

        override fun numFreeThenPricePerPeriod(res: Resources): String {
            val stringRes = when (recurringPricingPhase) {
                is SubscriptionPricingPhase.Years -> R.string.plus_trial_then_slash_year
                is SubscriptionPricingPhase.Months -> R.string.plus_trial_then_slash_month
            }
            return res.getString(
                stringRes,
                offerPricingPhase.periodValuePlural(res),
                recurringPricingPhase.formattedPrice,
            )
        }

        override fun tryFreeThenPricePerPeriod(res: Resources): String {
            val stringRes = when (recurringPricingPhase) {
                is SubscriptionPricingPhase.Years -> R.string.trial_then_per_year
                is SubscriptionPricingPhase.Months -> R.string.trial_then_per_month
            }
            return res.getString(
                stringRes,
                offerPricingPhase.periodValuePlural(res),
                recurringPricingPhase.formattedPrice,
            )
        }
    }

    class Intro(
        tier: SubscriptionTier,
        recurringPricingPhase: RecurringSubscriptionPricingPhase,
        offerPricingPhase: OfferSubscriptionPricingPhase,
        productDetails: ProductDetails,
        offerToken: String,
    ) : WithOffer(tier, recurringPricingPhase, offerPricingPhase, productDetails, offerToken) {
        override fun badgeOfferText(): Int = R.string.half_price_first_year

        override fun numFreeThenPricePerPeriod(res: Resources): String {
            return "TODO"
        }

        override fun tryFreeThenPricePerPeriod(res: Resources): String {
            return "TODO"
        }
    }

    enum class SubscriptionTier {
        PLUS,
        PATRON,
        UNKNOWN,
        ;

        companion object {
            fun fromUserTier(userTier: UserTier) = when (userTier) {
                UserTier.Free -> UNKNOWN
                UserTier.Plus -> PLUS
                UserTier.Patron -> PATRON
            }
        }
    }

    companion object {
        const val PLUS_PRODUCT_BASE = "com.pocketcasts.plus"
        const val PLUS_MONTHLY_PRODUCT_ID = "$PLUS_PRODUCT_BASE.monthly"
        const val PLUS_YEARLY_PRODUCT_ID = "$PLUS_PRODUCT_BASE.yearly"
        const val PATRON_MONTHLY_PRODUCT_ID = "com.pocketcasts.monthly.patron"
        const val PATRON_YEARLY_PRODUCT_ID = "com.pocketcasts.yearly.patron"
        const val SUBSCRIPTION_TEST_PRODUCT_ID = "com.pocketcasts.plus.testfreetrialoffer"
        const val TRIAL_OFFER_ID = "plus-yearly-trial-30days"
        const val INTRO_OFFER_ID = "testyearlyintropricingoffer"

        fun fromProductDetails(productDetails: ProductDetails, isFreeTrialEligible: Boolean): Subscription? =
            SubscriptionMapper.map(productDetails, isFreeTrialEligible)
    }
}
