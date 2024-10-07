package au.com.shiftyjelly.pocketcasts.referrals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfo
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfoPlayStore
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.EmptyResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.ErrorResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult.SuccessResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralOfferInfoProvider
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class ReferralsSendGuestPassViewModel @Inject constructor(
    private val referralsManager: ReferralManager,
    private val referralOfferInfoProvider: ReferralOfferInfoProvider,
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
            val referralsOfferInfo = referralOfferInfoProvider.referralOfferInfo() as? ReferralsOfferInfoPlayStore
            referralsOfferInfo?.subscriptionWithOffer?.let {
                val result = referralsManager.getReferralCode()
                _state.value = when (result) {
                    is SuccessResult -> UiState.Loaded(
                        code = result.body.code,
                        referralsOfferInfo = referralsOfferInfo,
                    )

                    is EmptyResult -> {
                        LogBuffer.e(
                            LogBuffer.TAG_INVALID_STATE,
                            "Empty result when getting referral code.",
                        )
                        UiState.Error(ReferralSendGuestPassError.Empty)
                    }

                    is ErrorResult -> {
                        if (result.error is NoNetworkException) {
                            UiState.Error(ReferralSendGuestPassError.NoNetwork)
                        } else {
                            LogBuffer.e(
                                LogBuffer.TAG_INVALID_STATE,
                                "Failed to get referral code ${result.errorMessage}",
                            )
                            UiState.Error(ReferralSendGuestPassError.FailedToLoad)
                        }
                    }
                }
            } ?: run {
                val message = "Failed to load referral offer info"
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, message)
                Timber.e(message)
                _state.value = UiState.Error(ReferralSendGuestPassError.FailedToLoad)
            }
        }
    }

    fun onRetry() {
        _state.value = UiState.Loading
        getReferralCode()
    }

    fun onShareClick(referralCode: String) {
        if (_state.value !is UiState.Loaded) return
        val request = SharingRequest.referralLink(
            referralCode = referralCode,
            referralsOfferInfo = (_state.value as UiState.Loaded).referralsOfferInfo,
        ).setSourceView(SourceView.REFERRALS)
            .build()
        viewModelScope.launch {
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
        data class Loaded(
            val code: String,
            val referralsOfferInfo: ReferralsOfferInfo,
        ) : UiState()

        data class Error(val error: ReferralSendGuestPassError) : UiState()
    }

    sealed class ReferralSendGuestPassError {
        data object FailedToLoad : ReferralSendGuestPassError()
        data object NoNetwork : ReferralSendGuestPassError()
        data object Empty : ReferralSendGuestPassError()
    }
}
