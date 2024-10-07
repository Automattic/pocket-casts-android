package au.com.shiftyjelly.pocketcasts.models.type

import com.android.billingclient.api.ProductDetails

object SubscriptionMapper {
    private var subscription: Subscription? = null

    fun map(
        productDetails: ProductDetails,
        isOfferEligible: Boolean,
        referralProductDetails: ReferralProductDetails?,
    ): Subscription? {
        return referralProductDetails?.let {
            subscription
        }
    }

    fun setSubscription(subscription: Subscription? = null) {
        this.subscription = subscription
    }
}
