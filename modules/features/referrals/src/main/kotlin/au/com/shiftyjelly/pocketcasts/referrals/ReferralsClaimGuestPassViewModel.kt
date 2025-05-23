package au.com.shiftyjelly.pocketcasts.referrals

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PurchaseResult
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.flatMap
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.payment.onFailure
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class ReferralsClaimGuestPassViewModel @Inject constructor(
    private val referralManager: ReferralManager,
    private val userManager: UserManager,
    private val paymentClient: PaymentClient,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state: StateFlow<UiState> = _state

    private val _navigationEvent: MutableSharedFlow<NavigationEvent> = MutableSharedFlow()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent

    private val _snackBarEvent: MutableSharedFlow<SnackbarEvent> = MutableSharedFlow()
    val snackBarEvent: SharedFlow<SnackbarEvent> = _snackBarEvent

    private var job: Job? = null

    init {
        loadReferralClaimOffer()
    }

    private fun loadReferralClaimOffer() {
        viewModelScope.launch {
            val referralPlan = paymentClient.loadSubscriptionPlans()
                .flatMap { plans -> plans.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.Referral) }
                .flatMap(ReferralSubscriptionPlan::create)
                .onFailure { code, message -> LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to load referral offer: $code, $message") }
                .getOrNull()
            _state.value = if (referralPlan != null) {
                UiState.Loaded(referralPlan)
            } else {
                UiState.Error(ReferralsClaimGuestPassError.FailedToLoadOffer)
            }
        }
    }

    fun onActivatePassClick() {
        analyticsTracker.track(AnalyticsEvent.REFERRAL_ACTIVATE_TAPPED)
        job = viewModelScope.launch {
            userManager.getSignInState().asFlow()
                .stateIn(viewModelScope)
                .collect {
                    if (it.isSignedIn) {
                        val loadedState = _state.value as? UiState.Loaded
                        updateLoading(true)
                        val referralClaimCode = settings.referralClaimCode.value
                        val result = referralManager.validateReferralCode(referralClaimCode)
                        updateLoading(false)

                        when (result) {
                            is ReferralResult.SuccessResult -> {
                                loadedState?.referralPlan?.let { plan ->
                                    _navigationEvent.emit(NavigationEvent.LaunchBillingFlow(plan))
                                }
                                job?.cancel()
                            }

                            is ReferralResult.EmptyResult -> {
                                settings.referralClaimCode.set("", false)
                                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Empty validation result for redeem code: ${settings.referralClaimCode.value}")
                                _navigationEvent.emit(NavigationEvent.InValidOffer)
                            }

                            is ReferralResult.ErrorResult -> {
                                if (result.error is NoNetworkException) {
                                    _snackBarEvent.emit(SnackbarEvent.NoNetwork)
                                } else {
                                    settings.referralClaimCode.set("", false)
                                    LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Validation failed for redeem code: ${settings.referralClaimCode.value} ${result.error}")
                                    _navigationEvent.emit(NavigationEvent.InValidOffer)
                                }
                            }
                        }
                    } else {
                        settings.showReferralWelcome.set(false, updateModifiedAt = false)
                        _navigationEvent.emit(NavigationEvent.LoginOrSignup)
                    }
                }
        }
    }

    fun launchBillingFlow(
        referralPlan: ReferralSubscriptionPlan,
        activity: Activity,
    ) {
        analyticsTracker.track(AnalyticsEvent.REFERRAL_PURCHASE_SHOWN)
        viewModelScope.launch {
            val purchaseResult = paymentClient.purchaseSubscriptionPlan(referralPlan.key, purchaseSource = "referrals", activity)
            when (purchaseResult) {
                PurchaseResult.Purchased -> {
                    analyticsTracker.track(AnalyticsEvent.REFERRAL_PURCHASE_SUCCESS)
                    redeemReferralCode(settings.referralClaimCode.value)
                }

                is PurchaseResult.Failure -> {
                    _snackBarEvent.emit(SnackbarEvent.PurchaseFailed)
                }

                is PurchaseResult.Cancelled -> Unit
            }
        }
    }

    private suspend fun redeemReferralCode(code: String) {
        updateLoading(true)
        val result = referralManager.redeemReferralCode(code)
        updateLoading(false)
        when (result) {
            is ReferralResult.SuccessResult -> {
                settings.referralClaimCode.set("", false)
                (_state.value as? UiState.Loaded)
                    ?.let { loadedState -> _state.update { loadedState.copy(flowComplete = true) } }
                _navigationEvent.emit(NavigationEvent.Close)
                if (settings.showReferralWelcome.value) {
                    settings.showReferralWelcome.set(false, updateModifiedAt = false)
                    _navigationEvent.emit(NavigationEvent.Welcome)
                }
                job?.cancel()
            }

            is ReferralResult.EmptyResult -> {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Empty redemption result for redeem code: ${settings.referralClaimCode.value}")
                _snackBarEvent.emit(SnackbarEvent.RedeemFailed)
            }

            is ReferralResult.ErrorResult -> {
                if (result.error is NoNetworkException) {
                    _snackBarEvent.emit(SnackbarEvent.NoNetwork)
                } else {
                    LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Redeem failed for redeem code: ${settings.referralClaimCode.value} ${result.error}")
                    _snackBarEvent.emit(SnackbarEvent.RedeemFailed)
                }
            }
        }
    }

    fun retry() {
        val errorState = _state.value as? UiState.Error ?: return
        when (errorState.error) {
            ReferralsClaimGuestPassError.FailedToLoadOffer -> {
                _state.value = UiState.Loading
                loadReferralClaimOffer()
            }
        }
    }

    private fun updateLoading(show: Boolean) {
        (_state.value as? UiState.Loaded)
            ?.let { loadedState -> _state.update { loadedState.copy(isLoading = show) } }
    }

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.REFERRAL_CLAIM_SCREEN_SHOWN)
    }

    fun onDispose() {
        val loadedState = state.value as? UiState.Loaded
        // No need to track not now event when page is auto-closed and disposed on flow complete
        if (loadedState?.flowComplete == false) {
            analyticsTracker.track(AnalyticsEvent.REFERRAL_NOT_NOW_TAPPED)
        }
    }

    sealed class NavigationEvent {
        data object LoginOrSignup : NavigationEvent()
        data object InValidOffer : NavigationEvent()
        data class LaunchBillingFlow(val plan: ReferralSubscriptionPlan) : NavigationEvent()
        data object Close : NavigationEvent()
        data object Welcome : NavigationEvent()
    }

    sealed class SnackbarEvent {
        data object NoNetwork : SnackbarEvent()
        data object PurchaseFailed : SnackbarEvent()
        data object RedeemFailed : SnackbarEvent()
    }

    sealed class UiState {
        data object Loading : UiState()

        data class Loaded(
            val referralPlan: ReferralSubscriptionPlan,
            val isLoading: Boolean = false,
            val flowComplete: Boolean = false,
        ) : UiState()

        data class Error(val error: ReferralsClaimGuestPassError) : UiState()
    }

    sealed class ReferralsClaimGuestPassError {
        data object FailedToLoadOffer : ReferralsClaimGuestPassError()
    }
}
