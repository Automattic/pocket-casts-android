package au.com.shiftyjelly.pocketcasts.repositories.subscription

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails

interface ProductDetailsInterceptor {
    fun intercept(result: BillingResult, details: List<ProductDetails>): Pair<BillingResult, List<ProductDetails>>
}
