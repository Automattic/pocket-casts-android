package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.viewmodel.NewsletterSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EnableNotificationsPromptViewModel @Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private var _stateFlow: MutableStateFlow<UiState> = MutableStateFlow(UiState.PreNewOnboardingState)
    val stateFlow: StateFlow<UiState> = _stateFlow

    private var _messagesFlow: MutableSharedFlow<UiMessage> = MutableSharedFlow()
    val messagesFlow: SharedFlow<UiMessage> = _messagesFlow

    init {
        if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION)) {
            _stateFlow.update {
                UiState.NewOnboardingState(
                    isNotificationsChecked = true,
                    isNewsletterChecked = true,
                )
            }
        }
    }

    fun onCtaClick() {
        analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_PERMISSIONS_ALLOW_TAPPED) // should we report the same?
        when (val state = stateFlow.value) {
            is UiState.PreNewOnboardingState -> {
                viewModelScope.launch {
                    _messagesFlow.emit(UiMessage.RequestPermission)
                }
            }

            is UiState.NewOnboardingState -> {
                analyticsTracker.track(
                    AnalyticsEvent.NEWSLETTER_OPT_IN_CHANGED,
                    mapOf(
                        "source" to NewsletterSource.WELCOME_NEW_ACCOUNT.analyticsValue,
                        "enabled" to state.isNewsletterChecked,
                    ),
                )
                settings.marketingOptIn.set(state.isNewsletterChecked, updateModifiedAt = true)
                viewModelScope.launch {
                    _messagesFlow.emit(
                        if (state.isNotificationsChecked) {
                            UiMessage.RequestPermission
                        } else {
                            UiMessage.Dismiss
                        },
                    )
                }
            }
        }
    }

    fun reportShown() {
        analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_PERMISSIONS_SHOWN)
    }

    fun reportNotificationsOptInShown() {
        analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_OPT_IN_SHOWN)
    }

    fun reportNotificationRequestResult(wasEnabled: Boolean) {
        analyticsTracker.track(if (wasEnabled) AnalyticsEvent.NOTIFICATIONS_OPT_IN_ALLOWED else AnalyticsEvent.NOTIFICATIONS_OPT_IN_DENIED)
    }

    fun handleDismissedByUser() {
        settings.notificationsPromptAcknowledged.set(value = true, updateModifiedAt = true)
        analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_PERMISSIONS_DISMISSED)
    }

    fun onNewsletterChanged(isChecked: Boolean) {
        _stateFlow.update {
            (it as? UiState.NewOnboardingState)?.copy(isNewsletterChecked = isChecked) ?: it
        }
    }

    fun onNotificationsChanged(isChecked: Boolean) {
        _stateFlow.update {
            (it as? UiState.NewOnboardingState)?.copy(isNotificationsChecked = isChecked) ?: it
        }
    }

    sealed interface UiState {
        object PreNewOnboardingState : UiState
        data class NewOnboardingState(
            val isNewsletterChecked: Boolean,
            val isNotificationsChecked: Boolean,
        ) : UiState
    }

    sealed interface UiMessage {
        object RequestPermission : UiMessage
        object Dismiss : UiMessage
    }
}
