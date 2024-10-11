package au.com.shiftyjelly.pocketcasts.referrals

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfo
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfoPlayStore
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralOfferInfoProvider
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchaseEvent
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber

@HiltViewModel
class ReferralsClaimGuestPassViewModel @Inject constructor(
    private val referralOfferInfoProvider: ReferralOfferInfoProvider,
    private val referralManager: ReferralManager,
    private val userManager: UserManager,
    private val subscriptionManager: SubscriptionManager,
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
            val referralsOfferInfo = referralOfferInfoProvider.referralOfferInfo() as? ReferralsOfferInfoPlayStore
            referralsOfferInfo?.subscriptionWithOffer?.let {
                _state.update {
                    UiState.Loaded(
                        referralsOfferInfo = referralsOfferInfo,
                    )
                }
            } ?: run {
                val message = "Failed to load referral offer info"
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, message)
                Timber.e(message)
                _state.update { UiState.Error(ReferralsClaimGuestPassError.FailedToLoadOffer) }
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
                                val offerInfo = loadedState?.referralsOfferInfo as? ReferralsOfferInfoPlayStore
                                offerInfo?.subscriptionWithOffer?.let { subscriptionWithOffer -> triggerBillingFlowAndObservePurchaseEvents(subscriptionWithOffer) }
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

    private suspend fun triggerBillingFlowAndObservePurchaseEvents(
        subscriptionWithOffer: Subscription.WithOffer,
    ) {
        _navigationEvent.emit(NavigationEvent.LaunchBillingFlow(subscriptionWithOffer))

        val purchaseEvent = subscriptionManager
            .observePurchaseEvents()
            .asFlow()
            .firstOrNull()

        when (purchaseEvent) {
            PurchaseEvent.Success -> {
                analyticsTracker.track(AnalyticsEvent.REFERRAL_PURCHASE_SUCCESS)
                redeemReferralCode(settings.referralClaimCode.value)
            }

            is PurchaseEvent.Cancelled -> {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "PurchaseEvent.Cancelled")
                // User cancelled subscription creation. Do nothing.
            }

            is PurchaseEvent.Failure -> {
                _snackBarEvent.emit(SnackbarEvent.PurchaseFailed)
            }

            null -> {
                Timber.e("Purchase event was null. This should never happen.")
            }
        }
    }

    fun launchBillingFlow(
        activity: Activity,
        subscriptionWithOffer: Subscription.WithOffer,
    ) {
        analyticsTracker.track(AnalyticsEvent.REFERRAL_PURCHASE_SHOWN)
        subscriptionManager.launchBillingFlow(
            activity,
            subscriptionWithOffer.productDetails,
            subscriptionWithOffer.offerToken,
        )
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
        data class LaunchBillingFlow(val subscriptionWithOffer: Subscription.WithOffer) : NavigationEvent()
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
            val referralsOfferInfo: ReferralsOfferInfo,
            val isLoading: Boolean = false,
            val flowComplete: Boolean = false,
        ) : UiState()

        data class Error(val error: ReferralsClaimGuestPassError) : UiState()
    }

    sealed class ReferralsClaimGuestPassError {
        data object FailedToLoadOffer : ReferralsClaimGuestPassError()
    }
}
