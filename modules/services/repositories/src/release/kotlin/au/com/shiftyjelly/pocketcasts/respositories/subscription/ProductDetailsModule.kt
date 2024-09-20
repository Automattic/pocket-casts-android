package au.com.shiftyjelly.pocketcasts.repositories.subscription

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ProductDetailsModule {
    @Provides
    fun provideProductDetailsInterceptor(): ProductDetailsInterceptor = NoOpProductDetailsInterceptor()
}

private class NoOpProductDetailsInterceptor : ProductDetailsInterceptor {
    override fun intercept(result: BillingResult, details: List<ProductDetails>): Pair<BillingResult, List<ProductDetails>> {
        return result to details
    }
}
