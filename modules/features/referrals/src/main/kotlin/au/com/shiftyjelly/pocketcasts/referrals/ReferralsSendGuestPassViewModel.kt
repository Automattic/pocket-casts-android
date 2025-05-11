package au.com.shiftyjelly.pocketcasts.referrals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.flatMap
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.payment.onFailure
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.EmptyResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.ErrorResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.SuccessResult
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ReferralsSendGuestPassViewModel @Inject constructor(
    private val referralsManager: ReferralManager,
    private val paymentClient: PaymentClient,
    private val sharingClient: SharingClient,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state: StateFlow<UiState> = _state

    init {
        getReferralCode()
    }

    private fun getReferralCode() {
        viewModelScope.launch {
            val referralPlan = paymentClient.loadSubscriptionPlans()
                .flatMap { plans -> plans.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.Referral) }
                .flatMap(ReferralSubscriptionPlan::create)
                .onFailure { code, message -> LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to load referral offer: $code, $message") }
                .getOrNull()
            _state.value = if (referralPlan != null) {
                val codeResult = referralsManager.getReferralCode()
                when (codeResult) {
                    is SuccessResult -> UiState.Loaded(
                        referralPlan = referralPlan,
                        code = codeResult.body.code,
                    )

                    is EmptyResult -> {
                        LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Empty result when getting referral code.")
                        UiState.Error(ReferralSendGuestPassError.Empty)
                    }

                    is ErrorResult -> {
                        if (codeResult.error is NoNetworkException) {
                            UiState.Error(ReferralSendGuestPassError.NoNetwork)
                        } else {
                            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to get referral code ${codeResult.errorMessage}")
                            UiState.Error(ReferralSendGuestPassError.FailedToLoad)
                        }
                    }
                }
            } else {
                UiState.Error(ReferralSendGuestPassError.FailedToLoad)
            }
        }
    }

    fun onRetry() {
        _state.value = UiState.Loading
        getReferralCode()
    }

    fun onShareClick(
        referralCode: String,
        offerName: String,
        offerDuration: String,
    ) {
        viewModelScope.launch {
            val request = SharingRequest.referralLink(referralCode, offerName, offerDuration)
                .setSourceView(SourceView.REFERRALS)
                .build()
            sharingClient.share(request)
        }
    }

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.REFERRAL_SHARE_SCREEN_SHOWN)
    }

    fun onDispose() {
        analyticsTracker.track(AnalyticsEvent.REFERRAL_SHARE_SCREEN_DISMISSED)
    }

    sealed class UiState {
        data object Loading : UiState()

        data class Error(val error: ReferralSendGuestPassError) : UiState()

        data class Loaded(
            val referralPlan: ReferralSubscriptionPlan,
            val code: String,
        ) : UiState()
    }

    sealed class ReferralSendGuestPassError {
        data object FailedToLoad : ReferralSendGuestPassError()
        data object NoNetwork : ReferralSendGuestPassError()
        data object Empty : ReferralSendGuestPassError()
    }
}
