package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.extensions.price
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UpgradeAccountViewModel
@Inject constructor(
    subscriptionManager: SubscriptionManager,
) : ViewModel() {
    private val productDetails = subscriptionManager.observeProductDetails().map {
        if (it is ProductDetailsState.Loaded) {
            val price =
                it.productDetails.find { detail -> detail.productId == SubscriptionManager.TEST_FREE_TRIAL_SKU }?.price
            if (price != null) {
                Optional.of(ProductState(price))
            } else {
                Optional.empty()
            }
        } else {
            Optional.empty()
        }
    }
    val productState: LiveData<Optional<ProductState>> =
        LiveDataReactiveStreams.fromPublisher(productDetails)

    data class ProductState(
        val price: String,
    )
}
