package au.com.shiftyjelly.pocketcasts.referrals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.flatMap
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.payment.onFailure
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class ReferralsViewModel @Inject constructor(
    private val userManager: UserManager,
    private val paymentClient: PaymentClient,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val referralPlan = paymentClient.loadSubscriptionPlans()
                .flatMap { plans -> plans.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.Referral) }
                .flatMap(ReferralSubscriptionPlan::create)
                .onFailure { code, message -> LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to load referral offer: $code, $message") }
                .getOrNull()
            if (referralPlan != null) {
                observeLoadedUiStates(referralPlan).collect { state ->
                    _state.value = state
                }
            } else {
                _state.value = UiState.NoOffer
            }
        }
    }

    private fun observeLoadedUiStates(referralPlan: ReferralSubscriptionPlan): Flow<UiState> {
        return combine(
            userManager.getSignInState().asFlow(),
            settings.playerOrUpNextBottomSheetState,
        ) { signInState, playerBottomSheetState ->
            val canClaimReferral = signInState.isNoAccountOrFree && settings.referralClaimCode.value.isNotEmpty()
            val canSendReferral = signInState.isSignedInAsPlusOrPatron
            UiState.Loaded(
                referralPlan = referralPlan,
                showIcon = canSendReferral,
                showTooltip = if (playerBottomSheetState == BottomSheetBehavior.STATE_COLLAPSED) {
                    canSendReferral && settings.showReferralsTooltip.value
                } else {
                    false
                },
                showProfileBanner = canClaimReferral,
            )
        }
    }

    fun onIconClick() {
        hideTooltip()
    }

    fun onTooltipClick() {
        hideTooltip()
        analyticsTracker.track(AnalyticsEvent.REFERRAL_TOOLTIP_TAPPED)
    }

    private fun hideTooltip() {
        if (settings.showReferralsTooltip.value) {
            settings.showReferralsTooltip.set(false, updateModifiedAt = false)
        }
        (_state.value as? UiState.Loaded)?.let { loadedState ->
            _state.update { loadedState.copy(showTooltip = false) }
        }
    }

    fun onTooltipShown() {
        if ((_state.value as? UiState.Loaded)?.showTooltip == true) {
            analyticsTracker.track(AnalyticsEvent.REFERRAL_TOOLTIP_SHOWN)
        }
    }

    fun onBannerShown() {
        if ((_state.value as? UiState.Loaded)?.showProfileBanner == true) {
            analyticsTracker.track(AnalyticsEvent.REFERRAL_PASS_BANNER_SHOWN)
        }
    }

    fun onHideBannerClick() {
        (_state.value as? UiState.Loaded)?.let { loadedState ->
            settings.referralClaimCode.set("", updateModifiedAt = false)
            analyticsTracker.track(AnalyticsEvent.REFERRAL_PASS_BANNER_HIDE_TAPPED)
            _state.update {
                loadedState.copy(
                    showProfileBanner = false,
                    showHideBannerPopup = false,
                )
            }
        }
    }

    sealed class UiState {
        data object Loading : UiState()

        data object NoOffer : UiState()

        data class Loaded(
            val referralPlan: ReferralSubscriptionPlan,
            val showIcon: Boolean = false,
            val showTooltip: Boolean = false,
            val showProfileBanner: Boolean = false,
            val showHideBannerPopup: Boolean = false,
        ) : UiState()
    }
}
