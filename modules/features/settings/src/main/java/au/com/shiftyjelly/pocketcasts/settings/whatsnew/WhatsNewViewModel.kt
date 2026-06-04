package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    private val settings: Settings,
) : ViewModel() {
    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state = _state.asStateFlow()

    private val _navigationState: MutableSharedFlow<NavigationState> = MutableSharedFlow()
    val navigationState = _navigationState.asSharedFlow()

    init {
        viewModelScope.launch {
            settings.cachedSubscription.flow.collect { subscription ->
                _state.value = UiState.Loaded(
                    feature = WhatsNewFeature.SyncedTranscripts(isUserEntitled = subscription != null),
                )
            }
        }
    }

    fun onConfirm() {
        viewModelScope.launch {
            val feature = (state.value as? UiState.Loaded)?.feature ?: return@launch
            val target = if (feature.isUserEntitled) {
                NavigationState.ForceClose
            } else {
                NavigationState.StartUpsellFlow(source = OnboardingUpgradeSource.SYNCED_TRANSCRIPTS)
            }
            _navigationState.emit(target)
        }
    }

    sealed class UiState {
        data object Loading : UiState()

        data class Loaded(
            val feature: WhatsNewFeature,
        ) : UiState()
    }

    sealed interface WhatsNewFeature {
        @get:StringRes val title: Int

        @get:StringRes val message: Int

        @get:StringRes val confirmButtonTitle: Int

        @get:StringRes val closeButtonTitle: Int? get() = null

        @get:StringRes val confirmButtonNote: Int? get() = null
        val isUserEntitled: Boolean
        val subscriptionTier: SubscriptionTier? get() = null

        data class SyncedTranscripts(
            override val isUserEntitled: Boolean,
        ) : WhatsNewFeature {
            override val title = LR.string.synced_transcripts_whats_new_title
            override val message = LR.string.synced_transcripts_whats_new_message
            override val confirmButtonTitle
                get() = if (isUserEntitled) LR.string.got_it else LR.string.profile_start_free_trial
            override val confirmButtonNote = LR.string.synced_transcripts_whats_new_button_note
            override val subscriptionTier get() = SubscriptionTier.Plus
        }
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
