package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WhatsNewViewModel @Inject constructor() : ViewModel() {
    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state = _state.asStateFlow()

    private val _navigationState: MutableSharedFlow<NavigationState> = MutableSharedFlow()
    val navigationState = _navigationState.asSharedFlow()

    fun onConfirm() {
        viewModelScope.launch {
            _navigationState.emit(NavigationState.ForceClose)
        }
    }

    sealed class UiState {
        data object Loading : UiState()
        data class Loaded(
            val feature: WhatsNewFeature,
            val tier: UserTier,
            val fullModel: Boolean = false,
        ) : UiState()
    }

    sealed interface WhatsNewFeature {
        @get:StringRes val title: Int

        @get:StringRes val message: Int

        @get:StringRes val confirmButtonTitle: Int

        @get:StringRes val closeButtonTitle: Int? get() = null
        val hasOffer: Boolean
        val isUserEntitled: Boolean
        val subscriptionTier: SubscriptionTier? // To show subscription when user is not entitled to the feature
    }

    sealed class NavigationState(
        open val shouldCloseOnConfirm: Boolean = true,
    ) {
        data class StartUpsellFlow(
            val source: OnboardingUpgradeSource,
            override val shouldCloseOnConfirm: Boolean = true,
        ) : NavigationState()

        data object ForceClose : NavigationState()
    }
}
