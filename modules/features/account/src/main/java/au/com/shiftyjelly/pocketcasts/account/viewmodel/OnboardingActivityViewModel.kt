package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivityContract.OnboardingFinish
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingExitInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class OnboardingActivityViewModel @Inject constructor(
    private val userManager: UserManager,
) : ViewModel() {

    private var showPlusPromotionForFreeUserFlow = MutableStateFlow(false)

    private val _finishState = MutableSharedFlow<OnboardingFinish>()
    val finishState = _finishState.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                showPlusPromotionForFreeUserFlow,
                userManager.getSignInState().asFlow(),
            ) { showPlusPromotionForFreeUser, signInState ->
                if (showPlusPromotionForFreeUser) {
                    // subscriptionStatus is null just after sign in, so we need to wait for it to be set
                    // before we can finish the onboarding flow to show plus promotion for a free user
                    (signInState as? SignInState.SignedIn)?.subscriptionStatus?.let { status ->
                        showPlusPromotionForFreeUserFlow.value = false
                        if (status is SubscriptionStatus.Free) {
                            _finishState.emit(OnboardingFinish.DoneShowPlusPromotion)
                        } else {
                            _finishState.emit(OnboardingFinish.Done)
                        }
                    }
                }
            }.stateIn(viewModelScope)
        }
    }

    fun onExitOnboarding(exitInfo: OnboardingExitInfo) {
        if (exitInfo.showPlusPromotionForFreeUser) {
            showPlusPromotionForFreeUserFlow.value = true
        } else {
            viewModelScope.launch {
                _finishState.emit(OnboardingFinish.Done)
            }
        }
    }
}
