package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.utils.Optional
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UpgradeAccountViewModel
@Inject constructor(
    subscriptionManager: SubscriptionManager
) : ViewModel() {
    private val productDetails = subscriptionManager.observeProductDetails().map {
        if (it is ProductDetailsState.Loaded) {
            val price = it.skuDetails.find { detail -> detail.sku == SubscriptionManager.MONTHLY_SKU }?.price
            if (price != null) {
                Optional.of(price)
            } else {
                Optional.empty()
            }
        } else {
            Optional.empty()
        }
    }
    val productState: LiveData<Optional<String>> = LiveDataReactiveStreams.fromPublisher(productDetails)
}
