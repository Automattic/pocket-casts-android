package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivityContract.OnboardingFinish
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingExitInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
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
                    when (signInState) {
                        is SignInState.SignedIn -> {
                            showPlusPromotionForFreeUserFlow.value = false
                            if (signInState.subscription != null) {
                                _finishState.emit(OnboardingFinish.Done)
                            } else {
                                _finishState.emit(OnboardingFinish.DoneShowPlusPromotion)
                            }
                        }

                        is SignInState.SignedOut -> Unit
                    }
                }
            }.collect()
        }
    }

    fun onExitOnboarding(exitInfo: OnboardingExitInfo) {
        when (exitInfo) {
            is OnboardingExitInfo.Simple -> {
                viewModelScope.launch {
                    _finishState.emit(OnboardingFinish.Done)
                }
            }

            is OnboardingExitInfo.ShowPlusPromotion -> {
                showPlusPromotionForFreeUserFlow.value = true
            }

            is OnboardingExitInfo.ShowReferralWelcome -> {
                viewModelScope.launch {
                    _finishState.emit(OnboardingFinish.DoneShowWelcomeInReferralFlow)
                }
            }

            is OnboardingExitInfo.ApplySuggestedFolders -> {
                viewModelScope.launch {
                    _finishState.emit(OnboardingFinish.DoneApplySuggestedFolders(exitInfo.action))
                }
            }
        }
    }
}
