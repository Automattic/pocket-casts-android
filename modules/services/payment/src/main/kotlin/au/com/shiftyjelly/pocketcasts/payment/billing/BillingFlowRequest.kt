package au.com.shiftyjelly.pocketcasts.payment.billing

import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails

internal data class BillingFlowRequest(
    val productQuery: ProductQuery,
    val subscriptionUpdateQuery: SubscriptionUpdateQuery?,
) {
    fun toGoogleParams(): BillingFlowParams {
        val replacementParams = subscriptionUpdateQuery?.let { query ->
            BillingFlowParams.ProductDetailsParams.SubscriptionProductReplacementParams.newBuilder()
                .setOldProductId(query.oldProductId)
                .setReplacementMode(query.replacementMode)
                .build()
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productQuery.product)
            .setOfferToken(productQuery.offerToken)
            .let { builder ->
                if (replacementParams != null) {
                    builder.setSubscriptionProductReplacementParams(replacementParams)
                } else {
                    builder
                }
            }
            .build()
        return BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
    }

    data class ProductQuery(
        val product: ProductDetails,
        val offerToken: String,
    )

    data class SubscriptionUpdateQuery(
        val oldProductId: String,
        val replacementMode: Int,
    )
}
