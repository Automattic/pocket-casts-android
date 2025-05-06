package au.com.shiftyjelly.pocketcasts.payment.billing

import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails

internal data class BillingFlowRequest(
    val productQuery: ProductQuery,
    val subscriptionUpdateQuery: SubscriptionUpdateQuery?,
) {
    fun toGoogleParams(): BillingFlowParams {
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productQuery.product)
            .setOfferToken(productQuery.offerToken)
            .build()
        val subscriptionUpdateParams = subscriptionUpdateQuery?.let { query ->
            BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(query.purchaseToken)
                .setSubscriptionReplacementMode(query.replacementMode)
                .build()
        }
        return BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .let { builder ->
                if (subscriptionUpdateParams != null) {
                    builder.setSubscriptionUpdateParams(subscriptionUpdateParams)
                } else {
                    builder
                }
            }
            .build()
    }

    data class ProductQuery(
        val product: ProductDetails,
        val offerToken: String,
    )

    data class SubscriptionUpdateQuery(
        val purchaseToken: String,
        val replacementMode: Int,
    )
}
