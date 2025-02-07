package au.com.shiftyjelly.pocketcasts.profile.winback

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.BillingPeriod
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.models.type.WinbackOfferDetails
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchaseEvent
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchasesState
import au.com.shiftyjelly.pocketcasts.repositories.winback.WinbackManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.pocketcasts.service.api.WinbackResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class WinbackViewModel @Inject constructor(
    private val winbackManager: WinbackManager,
    private val settings: Settings,
    private val tracker: AnalyticsTracker,
) : ViewModel() {
    private val subscriptionMapper = SubscriptionMapper()

    private val _uiState = MutableStateFlow(UiState.Empty)
    internal val uiState = _uiState.asStateFlow()

    init {
        loadWinbackData()
        viewModelScope.launch {
            settings.cachedSubscriptionStatus.flow.collect { status ->
                _uiState.value = _uiState.value.copy(currentSubscriptionExpirationDate = status?.expiryDate)
            }
        }
    }

    internal fun loadWinbackData() {
        _uiState.value = _uiState.value.copy(subscriptionPlansState = SubscriptionPlansState.Loading)
        viewModelScope.launch {
            val plansDeferred = async { loadPlans() }
            val activePurchaseDeferred = async { loadActivePurchase() }
            val winbackOfferResponseDeferred = async { loadWinbackOffer() }

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

            val winbackResponse = winbackOfferResponseDeferred.await()
            _uiState.value = _uiState.value.copy(
                winbackOfferState = winbackResponse
                    ?.toWinbackOffer(_uiState.value.productsDetails)
                    ?.let(::WinbackOfferState),
            )
        }
    }

    private var changePlanJob: Job? = null

    internal fun changePlan(
        newPlan: SubscriptionPlan,
        activity: Activity,
    ) {
        trackPlanSelected(newPlan.productId)
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
            _uiState.value = _uiState.value.withLoadedSubscriptionPlans { plans ->
                plans.copy(isChangingPlan = true)
            }
            val currentProductId = loadedState.activePurchase.productId
            val purchaseEvent = winbackManager.changeProduct(
                currentPurchase = currentPurchase,
                currentPurchaseProductId = currentProductId,
                newProduct = newProduct,
                newProductOfferToken = newPlan.offerToken,
                activity = activity,
            )
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
                    trackPlanPurchased(
                        currentProductId = currentProductId,
                        newProductId = newProduct.productId,
                    )

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
        }
    }

    private var claimOfferjob: Job? = null

    internal fun claimOffer(
        offer: WinbackOffer,
        activity: Activity,
    ) {
        trackClaimOfferTapped()
        if (claimOfferjob?.isActive == true) {
            return
        }

        val loadedState = (_uiState.value.subscriptionPlansState as? SubscriptionPlansState.Loaded) ?: run {
            logWarning("Failed to start winback offer flow. Subscriptions are not loaded.")
            return
        }
        val currentPurchase = _uiState.value.purchases.find { it.orderId == loadedState.activePurchase.orderId } ?: run {
            logWarning("Failed to start winback offer flow. No matching current purchase.")
            return
        }
        val winbackProduct = _uiState.value.productsDetails.find { product ->
            product.subscriptionOfferDetails.orEmpty().any { playOffer -> playOffer.offerToken == offer.offerToken }
        } ?: run {
            logWarning("Failed to start winback offer flow. No matching product for a winback offer.")
            return
        }
        claimOfferjob = viewModelScope.launch {
            _uiState.value = _uiState.value.withOfferState { state ->
                state.copy(isClaimingOffer = true)
            }
            val purchaseEvent = winbackManager.claimWinbackOffer(
                currentPurchase = currentPurchase,
                winbackProduct = winbackProduct,
                winbackOfferToken = offer.offerToken,
                winbackClaimCode = offer.redeemCode,
                activity = activity,
            )
            when (purchaseEvent) {
                is PurchaseEvent.Cancelled -> {
                    _uiState.value = _uiState.value.withOfferState { state ->
                        state.copy(isClaimingOffer = false)
                    }
                }

                is PurchaseEvent.Failure -> {
                    logWarning("Winback offer failure: ${purchaseEvent.responseCode}, ${purchaseEvent.errorMessage}")
                    _uiState.value = _uiState.value.withOfferState { state ->
                        state.copy(isClaimingOffer = false, hasOfferClaimFailed = true)
                    }
                }

                is PurchaseEvent.Success -> {
                    _uiState.value = _uiState.value.withOfferState { state ->
                        state.copy(isClaimingOffer = false, isOfferClaimed = true)
                    }
                }
            }
        }
    }

    internal fun consumeClaimedOffer() {
        _uiState.value = _uiState.value.withOfferState { state ->
            state.copy(isOfferClaimed = false)
        }
    }

    private suspend fun loadPlans() = when (val state = winbackManager.loadProducts()) {
        is ProductDetailsState.Loaded -> state.productDetails to state.productDetails.toSubscriptionPlans()
        is ProductDetailsState.Failure -> emptyList<ProductDetails>() to null
    }

    private suspend fun loadActivePurchase() = when (val state = winbackManager.loadPurchases()) {
        is PurchasesState.Loaded -> state.purchases to state.purchases.findActivePurchase()
        is PurchasesState.Failure -> emptyList<Purchase>() to ActivePurchaseResult.NotFound(FailureReason.Default)
    }

    private var winbackOfferJob: Deferred<WinbackResponse?>? = null

    // The winback offer is loaded this way to avoid plan change race conditions.
    // Changing a plan means the user may have a different winback offer available, which needs to be loaded.
    //
    // However, the offer from the initial call may not have loaded yet.
    //
    // If we start loading a new one without canceling the previous call, we might end up
    // displaying an incorrect winback offer to the user, as the old call could complete after the new one.
    private suspend fun loadWinbackOffer(): WinbackResponse? {
        winbackOfferJob?.cancelAndJoin()
        return viewModelScope.async { winbackManager.getWinbackOffer() }
            .also { winbackOfferJob = it }
            .await()
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

    private fun WinbackResponse.toWinbackOffer(products: List<ProductDetails>): WinbackOffer? {
        val offerId = offer.takeIf(String::isNotBlank) ?: return null
        val redeemCode = code.takeIf(String::isNotBlank) ?: return null
        val offerDetails = WinbackOfferDetails.fromOfferId(offerId) ?: return null
        val product = products.find { it.productId == offerDetails.productId } ?: return null
        val offer = product.subscriptionOfferDetails?.find { it.offerId == offerDetails.offerId } ?: return null
        val pricingPhases = offer.pricingPhases.pricingPhaseList.takeIf { it.size == 2 } ?: return null

        val discountPhase = pricingPhases[0]
        val regularPhase = pricingPhases[1]

        return WinbackOffer(
            details = offerDetails,
            offerToken = offer.offerToken,
            redeemCode = redeemCode,
            formattedPrice = when (offerDetails.billingPeriod) {
                BillingPeriod.Monthly -> regularPhase.formattedPrice
                BillingPeriod.Yearly -> discountPhase.formattedPrice
            },
        )
    }

    private fun UiState.withLoadedSubscriptionPlans(block: (SubscriptionPlansState.Loaded) -> SubscriptionPlansState): UiState {
        return if (subscriptionPlansState is SubscriptionPlansState.Loaded) {
            copy(subscriptionPlansState = block(subscriptionPlansState))
        } else {
            this
        }
    }

    private fun UiState.withOfferState(block: (WinbackOfferState) -> WinbackOfferState): UiState {
        return if (winbackOfferState != null) {
            copy(winbackOfferState = block(winbackOfferState))
        } else {
            this
        }
    }

    private fun logWarning(message: String) {
        Timber.tag(LogBuffer.TAG_SUBSCRIPTIONS).w(message)
        LogBuffer.w(LogBuffer.TAG_SUBSCRIPTIONS, message)
    }

    internal fun trackScreenShown(screen: String) {
        tracker.track(
            event = AnalyticsEvent.WINBACK_SCREEN_SHOWN,
            properties = mapOf("screen" to screen),
        )
    }

    internal fun trackScreenDismissed(screen: String) {
        tracker.track(
            event = AnalyticsEvent.WINBACK_SCREEN_DISMISSED,
            properties = mapOf("screen" to screen),
        )
    }

    internal fun trackContinueCancellationTapped() {
        tracker.track(
            event = AnalyticsEvent.WINBACK_CONTINUE_BUTTON_TAP,
        )
    }

    private fun trackClaimOfferTapped() {
        val activePurchase = (uiState.value.subscriptionPlansState as? SubscriptionPlansState.Loaded)?.activePurchase
        tracker.track(
            event = AnalyticsEvent.WINBACK_MAIN_SCREEN_ROW_TAP,
            properties = buildMap {
                put("row", "claim_offer")
                activePurchase?.tier?.let { tier ->
                    put("tier", tier)
                }
                activePurchase?.frequency?.let { frequency ->
                    put("frequency", frequency)
                }
            },
        )
    }

    internal fun trackAvailablePlansTapped() {
        tracker.track(
            event = AnalyticsEvent.WINBACK_MAIN_SCREEN_ROW_TAP,
            properties = mapOf(
                "row" to "available_plans",
            ),
        )
    }

    internal fun trackHelpAndFeedbackTapped() {
        tracker.track(
            event = AnalyticsEvent.WINBACK_MAIN_SCREEN_ROW_TAP,
            properties = mapOf(
                "row" to "help_and_feedback",
            ),
        )
    }

    internal fun trackOfferClaimedConfirmationTapped() {
        tracker.track(
            event = AnalyticsEvent.WINBACK_OFFER_CLAIMED_DONE_BUTTON_TAPPED,
        )
    }

    internal fun trackPlansBackButtonTapped() {
        tracker.track(
            event = AnalyticsEvent.WINBACK_AVAILABLE_PLANS_BACK_BUTTON_TAPPED,
        )
    }

    private fun trackPlanSelected(productId: String) {
        tracker.track(
            event = AnalyticsEvent.WINBACK_AVAILABLE_PLANS_SELECT_PLAN,
            properties = mapOf(
                "product" to productId,
            ),
        )
    }

    private fun trackPlanPurchased(currentProductId: String, newProductId: String) {
        tracker.track(
            event = AnalyticsEvent.WINBACK_AVAILABLE_PLANS_NEW_PLAN_PURCHASE_SUCCESSFUL,
            properties = mapOf(
                "current_product" to currentProductId,
                "new_product" to newProductId,
            ),
        )
    }

    internal fun trackKeepSubscriptionTapped() {
        tracker.track(
            event = AnalyticsEvent.WINBACK_CANCEL_CONFIRMATION_STAY_BUTTON_TAPPED,
        )
    }

    internal fun trackCancelSubscriptionTapped() {
        tracker.track(
            event = AnalyticsEvent.WINBACK_CANCEL_CONFIRMATION_CANCEL_BUTTON_TAPPED,
        )
    }

    internal data class UiState(
        val currentSubscriptionExpirationDate: Date?,
        val productsDetails: List<ProductDetails>,
        val purchases: List<Purchase>,
        val winbackOfferState: WinbackOfferState?,
        val subscriptionPlansState: SubscriptionPlansState,
    ) {
        val purchasedProductIds get() = purchases.flatMap { it.products }

        companion object {
            val Empty = UiState(
                currentSubscriptionExpirationDate = null,
                purchases = emptyList(),
                productsDetails = emptyList(),
                winbackOfferState = null,
                subscriptionPlansState = SubscriptionPlansState.Loading,
            )
        }
    }

    private sealed interface ActivePurchaseResult {
        data class Found(val purchase: ActivePurchase) : ActivePurchaseResult
        data class NotFound(val reason: FailureReason) : ActivePurchaseResult
    }
}

internal data class WinbackOfferState(
    val offer: WinbackOffer,
    val isClaimingOffer: Boolean = false,
    val isOfferClaimed: Boolean = false,
    val hasOfferClaimFailed: Boolean = false,
)

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

internal data class WinbackOffer(
    val details: WinbackOfferDetails,
    val offerToken: String,
    val redeemCode: String,
    val formattedPrice: String,
)

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
) {
    val tier get() = when (productId) {
        Subscription.PLUS_MONTHLY_PRODUCT_ID, Subscription.PLUS_YEARLY_PRODUCT_ID -> "plus"
        Subscription.PATRON_MONTHLY_PRODUCT_ID, Subscription.PATRON_YEARLY_PRODUCT_ID -> "patron"
        else -> null
    }

    val frequency get() = when (productId) {
        Subscription.PLUS_MONTHLY_PRODUCT_ID, Subscription.PATRON_MONTHLY_PRODUCT_ID -> "monthly"
        Subscription.PLUS_YEARLY_PRODUCT_ID, Subscription.PATRON_YEARLY_PRODUCT_ID -> "yearly"
        else -> null
    }
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
