package au.com.shiftyjelly.pocketcasts.referrals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfo
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfoPlayStore
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralOfferInfoProvider
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class ReferralsClaimGuestPassViewModel @Inject constructor(
    private val referralOfferInfoProvider: ReferralOfferInfoProvider,
) : ViewModel() {
    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state: StateFlow<UiState> = _state

    init {
        loadReferralClaimOffer()
    }

    private fun loadReferralClaimOffer() {
        viewModelScope.launch {
            val referralsOfferInfo = referralOfferInfoProvider.referralOfferInfo() as? ReferralsOfferInfoPlayStore?
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
                _state.update { UiState.Error }
            }
        }
    }

    fun retry() {
        loadReferralClaimOffer()
    }

    sealed class UiState {
        data object Loading : UiState()
        data class Loaded(val referralsOfferInfo: ReferralsOfferInfo) : UiState()
        data object Error : UiState()
    }
}
