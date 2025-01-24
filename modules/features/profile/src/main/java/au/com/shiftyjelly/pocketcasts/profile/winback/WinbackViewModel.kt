package au.com.shiftyjelly.pocketcasts.profile.winback

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchaseEvent
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchasesState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber

@HiltViewModel
class WinbackViewModel @Inject constructor(
    private val subscriptionManager: SubscriptionManager,
    private val settings: Settings,
) : ViewModel() {
    private val subscriptionMapper = SubscriptionMapper()

    private val _uiState = MutableStateFlow(UiState.Empty)
    internal val uiState = _uiState.asStateFlow()

    init {
        loadInitialPlans()
        viewModelScope.launch {
            settings.cachedSubscriptionStatus.flow.collect { status ->
                _uiState.value = _uiState.value.copy(currentSubscriptionExpirationDate = status?.expiryDate)
            }
        }
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
                        logWarning("Failed to load active purchase: ${activePurchase.reason}")
                        SubscriptionPlansState.Failure(activePurchase.reason)
                    }
                },
            )
        }
    }

    private var changePlanJob: Job? = null

    internal fun changePlan(
        activity: AppCompatActivity,
        newPlan: SubscriptionPlan,
    ) {
        if (changePlanJob?.isActive == true) {
            return
        }

        val loadedState = (_uiState.value.subscriptionPlansState as? SubscriptionPlansState.Loaded) ?: run {
            logWarning("Failed to start change product flow. Subscriptions are not loaded.")
            return
        }
        val currentPurchase = _uiState.value.purchases.find { it.orderId == loadedState.activePurchase.orderId } ?: run {
            logWarning("Failed to start change product flow. No matching current purchase.")
            return
        }
        val newProduct = _uiState.value.productsDetails.find { it.productId == newPlan.productId } ?: run {
            logWarning("Failed to start change product flow. No matching new product.")
            return
        }

        changePlanJob = viewModelScope.launch {
            val isChangeFlowStarted = subscriptionManager.changeProduct(
                currentPurchase = currentPurchase,
                currentPurchaseProductId = loadedState.activePurchase.productId,
                newProduct = newProduct,
                newProductOfferToken = newPlan.offerToken,
                activity = activity,
            )
            if (isChangeFlowStarted) {
                _uiState.value = _uiState.value.withLoadedSubscriptionPlans { plans ->
                    plans.copy(isChangingPlan = true)
                }

                val purchaseEvent = subscriptionManager.observePurchaseEvents().asFlow().first()
                when (purchaseEvent) {
                    is PurchaseEvent.Cancelled -> {
                        _uiState.value = _uiState.value.withLoadedSubscriptionPlans { plans ->
                            plans.copy(isChangingPlan = false)
                        }
                    }

                    is PurchaseEvent.Failure -> {
                        logWarning("Purchase failure: ${purchaseEvent.responseCode}, ${purchaseEvent.errorMessage}")
                        _uiState.value = _uiState.value.withLoadedSubscriptionPlans { plans ->
                            plans.copy(isChangingPlan = false, hasPlanChangeFailed = true)
                        }
                    }

                    is PurchaseEvent.Success -> {
                        val (newPurchases, newPurchase) = loadActivePurchase()
                        when (newPurchase) {
                            is ActivePurchaseResult.Found -> {
                                _uiState.value = _uiState.value
                                    .copy(purchases = newPurchases)
                                    .withLoadedSubscriptionPlans { plans ->
                                        plans.copy(isChangingPlan = false, activePurchase = newPurchase.purchase)
                                    }
                            }
                            is ActivePurchaseResult.NotFound -> {
                                val failure = SubscriptionPlansState.Failure(FailureReason.Default)
                                _uiState.value = _uiState.value.copy(subscriptionPlansState = failure)
                            }
                        }
                    }
                }
            } else {
                logWarning("Failed to start change product flow.")
                _uiState.value = _uiState.value.withLoadedSubscriptionPlans { plans ->
                    plans.copy(hasPlanChangeFailed = true)
                }
            }
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
        val acknowledgedPurchases = filter { it.isAcknowledged && it.isAutoRenewing }

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

    private fun UiState.withLoadedSubscriptionPlans(block: (SubscriptionPlansState.Loaded) -> SubscriptionPlansState): UiState {
        return if (subscriptionPlansState is SubscriptionPlansState.Loaded) {
            copy(subscriptionPlansState = block(subscriptionPlansState))
        } else {
            this
        }
    }

    private fun logWarning(message: String) {
        Timber.tag(LogBuffer.TAG_SUBSCRIPTIONS).w(message)
        LogBuffer.w(LogBuffer.TAG_SUBSCRIPTIONS, message)
    }

    internal data class UiState(
        val currentSubscriptionExpirationDate: Date?,
        val productsDetails: List<ProductDetails>,
        val purchases: List<Purchase>,
        val subscriptionPlansState: SubscriptionPlansState,
    ) {
        val purchasedProductIds get() = purchases.flatMap { it.products }

        companion object {
            val Empty = UiState(
                currentSubscriptionExpirationDate = null,
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
        val isChangingPlan: Boolean = false,
        val hasPlanChangeFailed: Boolean = false,
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
