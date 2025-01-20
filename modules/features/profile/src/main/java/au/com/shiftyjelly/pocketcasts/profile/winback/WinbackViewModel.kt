package au.com.shiftyjelly.pocketcasts.profile.winback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchasesState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WinbackViewModel @Inject constructor(
    val subscriptionManager: SubscriptionManager,
) : ViewModel() {
    private val subscriptionMapper = SubscriptionMapper()

    private val _uiState = MutableStateFlow(UiState.Empty)
    internal val uiState = _uiState.asStateFlow()

    init {
        loadInitialPlans()
    }

    internal fun loadInitialPlans() {
        _uiState.value = _uiState.value.copy(subscriptionPlansState = SubscriptionPlansState.Loading)
        viewModelScope.launch {
            val plansDeferred = async { loadPlans() }
            val activePurchaseDeferred = async { loadActivePurchase() }

            val plansResult = plansDeferred.await()
            val activePurchaseResult = activePurchaseDeferred.await()

            val productsDetails = plansResult.first
            val plans = plansResult.second
            val purchases = activePurchaseResult.first
            val activePurchase = activePurchaseResult.second

            _uiState.value = _uiState.value.copy(
                productsDetails = productsDetails,
                purchases = purchases,
                subscriptionPlansState = when (activePurchase) {
                    is ActivePurchaseResult.Found -> if (plans != null) {
                        SubscriptionPlansState.Loaded(activePurchase.purchase, plans)
                    } else {
                        SubscriptionPlansState.Failure(FailureReason.Default)
                    }

                    is ActivePurchaseResult.NotFound -> {
                        LogBuffer.w(LogBuffer.TAG_SUBSCRIPTIONS, "Failed to load active purchase: ${activePurchase.reason}")
                        SubscriptionPlansState.Failure(activePurchase.reason)
                    }
                },
            )
        }
    }

    private suspend fun loadPlans() = when (val state = subscriptionManager.loadProducts()) {
        is ProductDetailsState.Loaded -> state.productDetails to state.productDetails.toSubscriptionPlans()
        is ProductDetailsState.Failure -> emptyList<ProductDetails>() to null
    }

    private suspend fun loadActivePurchase() = when (val state = subscriptionManager.loadPurchases()) {
        is PurchasesState.Loaded -> state.purchases to state.purchases.findActivePurchase()
        is PurchasesState.Failure -> emptyList<Purchase>() to ActivePurchaseResult.NotFound(FailureReason.Default)
    }

    private fun List<ProductDetails>.toSubscriptionPlans() = map(subscriptionMapper::mapFromProductDetails)
        .filterIsInstance<Subscription.Simple>()
        .map(Subscription.Simple::toPlan)
        .sortedWith(PlanComparator)

    private fun List<Purchase>.findActivePurchase(): ActivePurchaseResult {
        val acknowledgedPurchases = filter(Purchase::isAcknowledged)

        if (acknowledgedPurchases.isEmpty()) {
            return ActivePurchaseResult.NotFound(FailureReason.NoPurchases)
        }
        if (acknowledgedPurchases.size > 1) {
            return ActivePurchaseResult.NotFound(FailureReason.TooManyPurchases)
        }

        val purchase = acknowledgedPurchases.single()
        val orderId = purchase.orderId

        return when {
            orderId == null -> {
                ActivePurchaseResult.NotFound(FailureReason.NoOrderId)
            }

            purchase.products.isEmpty() -> {
                ActivePurchaseResult.NotFound(FailureReason.NoProducts)
            }

            purchase.products.size == 1 -> {
                ActivePurchaseResult.Found(ActivePurchase(orderId, purchase.products.single()))
            }

            else -> {
                ActivePurchaseResult.NotFound(FailureReason.TooManyProducts)
            }
        }
    }

    internal data class UiState(
        val productsDetails: List<ProductDetails>,
        val purchases: List<Purchase>,
        val subscriptionPlansState: SubscriptionPlansState,
    ) {
        companion object {
            val Empty = UiState(
                purchases = emptyList(),
                productsDetails = emptyList(),
                subscriptionPlansState = SubscriptionPlansState.Loading,
            )
        }
    }

    private sealed interface ActivePurchaseResult {
        data class Found(val purchase: ActivePurchase) : ActivePurchaseResult
        data class NotFound(val reason: FailureReason) : ActivePurchaseResult
    }
}

internal sealed interface SubscriptionPlansState {
    data object Loading : SubscriptionPlansState

    data class Failure(
        val reason: FailureReason,
    ) : SubscriptionPlansState

    data class Loaded(
        val activePurchase: ActivePurchase,
        val plans: List<SubscriptionPlan>,
    ) : SubscriptionPlansState
}

internal data class SubscriptionPlan(
    val productId: String,
    val offerToken: String,
    val title: String,
    val formattedPrice: String,
    val billingPeriod: BillingPeriod,
)

internal data class ActivePurchase(
    val orderId: String,
    val productId: String,
)

internal enum class BillingPeriod {
    Monthly,
    Yearly,
}

internal enum class FailureReason {
    NoPurchases,
    TooManyPurchases,
    NoProducts,
    TooManyProducts,
    NoOrderId,
    Default,
}

private fun Subscription.Simple.toPlan() = SubscriptionPlan(
    productId = productDetails.productId,
    offerToken = offerToken,
    title = shortTitle,
    formattedPrice = recurringPricingPhase.formattedPrice,
    billingPeriod = when (recurringPricingPhase) {
        is SubscriptionPricingPhase.Months -> BillingPeriod.Monthly
        is SubscriptionPricingPhase.Years -> BillingPeriod.Yearly
    },
)

private object PlanComparator : Comparator<SubscriptionPlan> {
    private val priorities = mapOf(
        Subscription.PLUS_MONTHLY_PRODUCT_ID to 0,
        Subscription.PATRON_MONTHLY_PRODUCT_ID to 1,
        Subscription.PLUS_YEARLY_PRODUCT_ID to 2,
        Subscription.PATRON_YEARLY_PRODUCT_ID to 3,
    )

    override fun compare(o1: SubscriptionPlan, o2: SubscriptionPlan): Int {
        val priority1 = priorities[o1.productId]
        val priority2 = priorities[o2.productId]
        return when {
            priority1 != null && priority2 != null -> priority1 - priority2
            priority1 != null && priority2 == null -> -1
            priority1 == null && priority2 != null -> 1
            else -> o1.title.compareTo(o2.title)
        }
    }
}
