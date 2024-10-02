package au.com.shiftyjelly.pocketcasts.referrals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfo
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfoPlayStore
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralOfferInfoProvider
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val settings: Settings,
) : ViewModel() {
    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state: StateFlow<UiState> = _state

    private val _navigationEvent: MutableSharedFlow<NavigationEvent> = MutableSharedFlow()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent

    private val _snackBarEvent: MutableSharedFlow<SnackbarEvent> = MutableSharedFlow()
    val snackBarEvent: SharedFlow<SnackbarEvent> = _snackBarEvent

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
        viewModelScope.launch {
            userManager.getSignInState().asFlow()
                .stateIn(viewModelScope)
                .collect {
                    if (it.isSignedIn) {
                        val loadedState = _state.value as? UiState.Loaded
                        loadedState?.let { _state.update { loadedState.copy(isValidating = true) } }
                        val referralClaimCode = settings.referralClaimCode.value
                        val result = referralManager.validateReferralCode(referralClaimCode)
                        loadedState?.let { _state.update { loadedState.copy(isValidating = false) } }
                        when (result) {
                            is ReferralResult.SuccessResult -> startIAPFlow()
                            is ReferralResult.EmptyResult -> _navigationEvent.emit(NavigationEvent.InValidOffer)
                            is ReferralResult.ErrorResult -> if (result.error is NoNetworkException) {
                                _snackBarEvent.emit(SnackbarEvent.NoNetwork)
                            } else {
                                _navigationEvent.emit(NavigationEvent.InValidOffer)
                            }
                        }
                    } else {
                        _navigationEvent.emit(NavigationEvent.LoginOrSignup)
                    }
                }
        }
    }

    private fun startIAPFlow() {
        // TODO - Referrals: Implement IAP flow
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

    sealed class NavigationEvent {
        data object LoginOrSignup : NavigationEvent()
        data object InValidOffer : NavigationEvent()
    }

    sealed class SnackbarEvent {
        data object NoNetwork : SnackbarEvent()
    }

    sealed class UiState {
        data object Loading : UiState()
        data class Loaded(
            val referralsOfferInfo: ReferralsOfferInfo,
            val isValidating: Boolean = false,
        ) : UiState()

        data class Error(val error: ReferralsClaimGuestPassError) : UiState()
    }

    sealed class ReferralsClaimGuestPassError {
        data object FailedToLoadOffer : ReferralsClaimGuestPassError()
    }
}
