package au.com.shiftyjelly.pocketcasts.referrals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfo
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfoPlayStore
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralOfferInfoProvider
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber

@HiltViewModel
class ReferralsViewModel @Inject constructor(
    private val userManager: UserManager,
    private val referralOfferInfoProvider: ReferralOfferInfoProvider,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            val referralsOfferInfo = referralOfferInfoProvider.referralOfferInfo() as? ReferralsOfferInfoPlayStore
            referralsOfferInfo?.subscriptionWithOffer?.let {
                combine(
                    userManager.getSignInState().asFlow(),
                    settings.playerOrUpNextBottomSheetState,
                ) { signInState, playerBottomSheetState ->
                    val eligibleToSendPass = FeatureFlag.isEnabled(Feature.REFERRALS_SEND) && signInState.isSignedInAsPlusOrPatron
                    val eligibleToClaimPass = FeatureFlag.isEnabled(Feature.REFERRALS_CLAIM) &&
                        (!signInState.isSignedIn || signInState.isSignedInAsFree) &&
                        settings.referralClaimCode.value.isNotEmpty()
                    _state.update {
                        UiState.Loaded(
                            showIcon = eligibleToSendPass,
                            showTooltip = if (playerBottomSheetState == BottomSheetBehavior.STATE_COLLAPSED) {
                                eligibleToSendPass && settings.showReferralsTooltip.value
                            } else {
                                false
                            },
                            showProfileBanner = eligibleToClaimPass,
                            referralsOfferInfo = referralsOfferInfo,
                        )
                    }
                }.stateIn(this)
            } ?: run {
                val message = "Failed to load referral offer info"
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, message)
                Timber.e(message)
                _state.update {
                    UiState.Loaded(
                        showIcon = false,
                        showTooltip = false,
                        showProfileBanner = false,
                    )
                }
            }
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
        data class Loaded(
            val showIcon: Boolean = false,
            val showTooltip: Boolean = false,
            val showProfileBanner: Boolean = false,
            val showHideBannerPopup: Boolean = false,
            val referralsOfferInfo: ReferralsOfferInfo? = null,
        ) : UiState()
    }
}
