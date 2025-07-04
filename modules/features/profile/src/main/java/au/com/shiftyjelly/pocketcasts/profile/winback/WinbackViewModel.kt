package au.com.shiftyjelly.pocketcasts.profile.winback

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.payment.AcknowledgedSubscription
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.PurchaseResult
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.pocketcasts.service.api.WinbackResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
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
    private val paymentClient: PaymentClient,
    private val referralManager: ReferralManager,
    private val settings: Settings,
    private val tracker: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState.Empty)
    internal val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.cachedSubscription.flow.collect { subscription ->
                _uiState.value = _uiState.value.copy(currentSubscriptionExpirationDate = subscription?.expiryDate)
            }
        }
    }

    internal fun loadWinbackData() {
        _uiState.value = _uiState.value.copy(subscriptionPlansState = SubscriptionPlansState.Loading)
        viewModelScope.launch {
            val plansDeferred = async { loadPlans() }
            val activeSubscriptionDeferred = async { loadActiveSubscription() }
            val winbackOfferResponseDeferred = async { loadWinbackOffer() }

            val plans = plansDeferred.await()
            val activeSubscriptionResult = activeSubscriptionDeferred.await()

            _uiState.value = _uiState.value.copy(
                subscriptionPlansState = when (activeSubscriptionResult) {
                    is ActiveSubscriptionResult.Found -> if (plans != null) {
                        SubscriptionPlansState.Loaded(activeSubscriptionResult.subscription, plans)
                    } else {
                        SubscriptionPlansState.Failure(FailureReason.Default)
                    }

                    is ActiveSubscriptionResult.NotFound -> {
                        logWarning("Failed to load active purchase: ${activeSubscriptionResult.reason}")
                        SubscriptionPlansState.Failure(activeSubscriptionResult.reason)
                    }
                },
            )

            val winbackResponse = winbackOfferResponseDeferred.await()
            _uiState.value = _uiState.value.applyWinbackResponse(winbackResponse)
        }
    }

    private var changePlanJob: Job? = null

    internal fun changePlan(
        newPlan: SubscriptionPlan.Base,
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

        changePlanJob = viewModelScope.launch {
            _uiState.value = _uiState.value.withLoadedSubscriptionPlans { plans ->
                plans.copy(isChangingPlan = true)
            }
            when (paymentClient.purchaseSubscriptionPlan(newPlan.key, purchaseSource = "winback", activity)) {
                is PurchaseResult.Cancelled -> {
                    _uiState.value = _uiState.value.withLoadedSubscriptionPlans { plans ->
                        plans.copy(isChangingPlan = false)
                    }
                }

                is PurchaseResult.Failure -> {
                    _uiState.value = _uiState.value.withLoadedSubscriptionPlans { plans ->
                        plans.copy(isChangingPlan = false, hasPlanChangeFailed = true)
                    }
                }

                is PurchaseResult.Purchased -> {
                    trackPlanPurchased(
                        currentProductId = loadedState.currentSubscription.productId,
                        newProductId = newPlan.productId,
                    )
                    val activeSubscriptionDeferred = async { loadActiveSubscription() }
                    val winbackOfferResponseDeferred = async { loadWinbackOffer() }

                    val newPurchase = activeSubscriptionDeferred.await()

                    _uiState.value = when (newPurchase) {
                        is ActiveSubscriptionResult.Found -> {
                            _uiState.value
                                .copy(winbackOfferState = null)
                                .withLoadedSubscriptionPlans { plans ->
                                    plans.copy(isChangingPlan = false, currentSubscription = newPurchase.subscription)
                                }
                        }

                        is ActiveSubscriptionResult.NotFound -> {
                            val failure = SubscriptionPlansState.Failure(FailureReason.Default)
                            uiState.value.copy(subscriptionPlansState = failure, winbackOfferState = null)
                        }
                    }

                    val winbackResponse = winbackOfferResponseDeferred.await()
                    _uiState.value = _uiState.value.applyWinbackResponse(winbackResponse)
                }
            }
        }
    }

    private var claimOfferjob: Job? = null

    internal fun claimOffer(
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
        val winbackState = _uiState.value.winbackOfferState ?: run {
            logWarning("Failed to start winback offer flow. Winback offer is not loaded.")
            return
        }

        claimOfferjob = viewModelScope.launch {
            _uiState.value = _uiState.value.withOfferState { state ->
                state.copy(isClaimingOffer = true)
            }
            val newPlanKey = SubscriptionPlan.Key(
                loadedState.currentSubscription.tier,
                loadedState.currentSubscription.billingCycle,
                SubscriptionOffer.Winback,
            )
            when (paymentClient.purchaseSubscriptionPlan(newPlanKey, purchaseSource = "winback", activity)) {
                is PurchaseResult.Purchased -> {
                    referralManager.redeemReferralCode(winbackState.offer.redeemCode)
                    _uiState.value = _uiState.value.withOfferState { state ->
                        state.copy(isClaimingOffer = false, isOfferClaimed = true)
                    }
                }

                is PurchaseResult.Cancelled -> {
                    _uiState.value = _uiState.value.withOfferState { state ->
                        state.copy(isClaimingOffer = false)
                    }
                }

                is PurchaseResult.Failure -> {
                    _uiState.value = _uiState.value.withOfferState { state ->
                        state.copy(isClaimingOffer = false, hasOfferClaimFailed = true)
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

    private suspend fun loadPlans() = paymentClient.loadSubscriptionPlans().getOrNull()

    private suspend fun loadActiveSubscription() = when (val result = paymentClient.loadAcknowledgedSubscriptions()) {
        is PaymentResult.Success -> result.value.findActiveSubscription()
        is PaymentResult.Failure -> ActiveSubscriptionResult.NotFound(FailureReason.Default)
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
        return viewModelScope.async {
            when (val result = referralManager.getWinbackResponse()) {
                is ReferralResult.SuccessResult -> result.body
                is ReferralResult.EmptyResult -> null
                is ReferralResult.ErrorResult -> null
            }
        }.also { winbackOfferJob = it }.await()
    }

    private fun List<AcknowledgedSubscription>.findActiveSubscription(): ActiveSubscriptionResult {
        val activeSubscriptions = filter { it.isAutoRenewing }

        if (activeSubscriptions.isEmpty()) {
            return ActiveSubscriptionResult.NotFound(FailureReason.NoPurchases)
        }
        if (activeSubscriptions.size > 1) {
            return ActiveSubscriptionResult.NotFound(FailureReason.TooManyPurchases)
        }

        return ActiveSubscriptionResult.Found(activeSubscriptions.single())
    }

    private fun WinbackResponse.toWinbackOffer(
        plans: SubscriptionPlans,
    ): WinbackOffer? {
        val redeemCode = code.takeIf(String::isNotBlank) ?: return null
        val plan = plans.finMatchingWinbackPlan(offer) ?: return null
        val (discountPhase, regularPhase) = plan.pricingPhases.takeIf { it.size == 2 } ?: return null

        return WinbackOffer(
            redeemCode = redeemCode,
            formattedPrice = when (plan.billingCycle) {
                BillingCycle.Monthly -> regularPhase.price.formattedPrice
                BillingCycle.Yearly -> discountPhase.price.formattedPrice
            },
            tier = plan.tier,
            billingCycle = plan.billingCycle,
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

    private fun UiState.applyWinbackResponse(response: WinbackResponse?): UiState {
        val state = (subscriptionPlansState as? SubscriptionPlansState.Loaded)

        return copy(
            winbackOfferState = state
                ?.subscriptionPlans
                ?.let { response?.toWinbackOffer(it) }
                ?.takeIf { it.productId == state.currentSubscription.productId }
                ?.let(::WinbackOfferState),
        )
    }

    private fun SubscriptionPlans.finMatchingWinbackPlan(offerId: String): SubscriptionPlan.WithOffer? {
        SubscriptionTier.entries.forEach { tier ->
            BillingCycle.entries.forEach { billingCycle ->
                val offer = findOfferPlan(tier, billingCycle, SubscriptionOffer.Winback).getOrNull()
                if (offer != null && offer.offerId == offerId) {
                    return offer
                }
            }
        }
        return null
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
        val currentScubscription = (uiState.value.subscriptionPlansState as? SubscriptionPlansState.Loaded)?.currentSubscription
        tracker.track(
            event = AnalyticsEvent.WINBACK_MAIN_SCREEN_ROW_TAP,
            properties = buildMap {
                put("row", "claim_offer")
                currentScubscription?.tier?.let { tier ->
                    put("tier", tier.analyticsValue)
                }
                currentScubscription?.billingCycle?.let { billingCycle ->
                    put("frequency", billingCycle.analyticsValue)
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

    internal fun trackContinueWithCancellationTapped() {
        tracker.track(
            event = AnalyticsEvent.WINBACK_WINBACK_OFFER_CANCEL_BUTTON_TAPPED,
        )
    }

    internal data class UiState(
        val currentSubscriptionExpirationDate: Instant?,
        val winbackOfferState: WinbackOfferState?,
        val subscriptionPlansState: SubscriptionPlansState,
    ) {
        companion object {
            val Empty = UiState(
                currentSubscriptionExpirationDate = null,
                winbackOfferState = null,
                subscriptionPlansState = SubscriptionPlansState.Loading,
            )
        }
    }

    private sealed interface ActiveSubscriptionResult {
        data class Found(val subscription: AcknowledgedSubscription) : ActiveSubscriptionResult
        data class NotFound(val reason: FailureReason) : ActiveSubscriptionResult
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
        val currentSubscription: AcknowledgedSubscription,
        val subscriptionPlans: SubscriptionPlans,
        val isChangingPlan: Boolean = false,
        val hasPlanChangeFailed: Boolean = false,
    ) : SubscriptionPlansState {
        val basePlans get() = listOf(
            subscriptionPlans.getBasePlan(SubscriptionTier.Plus, BillingCycle.Monthly),
            subscriptionPlans.getBasePlan(SubscriptionTier.Patron, BillingCycle.Monthly),
            subscriptionPlans.getBasePlan(SubscriptionTier.Plus, BillingCycle.Yearly),
            subscriptionPlans.getBasePlan(SubscriptionTier.Patron, BillingCycle.Yearly),
        )
    }
}

internal data class WinbackOffer(
    val redeemCode: String,
    val formattedPrice: String,
    val tier: SubscriptionTier,
    val billingCycle: BillingCycle,
) {
    val productId get() = SubscriptionPlan.productId(tier, billingCycle)
}

internal enum class FailureReason {
    NoPurchases,
    TooManyPurchases,
    Default,
}
