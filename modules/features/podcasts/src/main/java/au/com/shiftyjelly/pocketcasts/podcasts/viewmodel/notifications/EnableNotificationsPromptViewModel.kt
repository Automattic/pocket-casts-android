package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.NewsletterOptInChangedEvent
import com.automattic.eventhorizon.NewsletterSourceType
import com.automattic.eventhorizon.NotificationsOptInAllowedEvent
import com.automattic.eventhorizon.NotificationsOptInDeniedEvent
import com.automattic.eventhorizon.NotificationsOptInShownEvent
import com.automattic.eventhorizon.NotificationsPermissionsAllowTappedEvent
import com.automattic.eventhorizon.NotificationsPermissionsNotNowTappedEvent
import com.automattic.eventhorizon.NotificationsPermissionsShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class EnableNotificationsPromptViewModel @Inject constructor(
    private val settings: Settings,
    private val eventHorizon: EventHorizon,
    private val userManager: UserManager,
) : ViewModel() {

    private var _stateFlow: MutableStateFlow<UiState> = MutableStateFlow(UiState.PreNewOnboarding)
    val stateFlow: StateFlow<UiState> = _stateFlow

    private var _messagesFlow: MutableSharedFlow<UiMessage> = MutableSharedFlow()
    val messagesFlow: SharedFlow<UiMessage> = _messagesFlow

    init {
        viewModelScope.launch {
            _stateFlow.update {
                UiState.NewOnboarding(
                    showNewsletterOptIn = userManager.getSignInState().asFlow().first().isSignedIn && !settings.marketingOptIn.value,
                    notificationsEnabled = true,
                    subscribedToNewsletter = true,
                )
            }
        }
    }

    fun handleCtaClick() {
        eventHorizon.track(NotificationsPermissionsAllowTappedEvent)
        when (val state = stateFlow.value) {
            is UiState.PreNewOnboarding -> {
                viewModelScope.launch {
                    _messagesFlow.emit(UiMessage.RequestPermission)
                }
            }

            is UiState.NewOnboarding -> {
                eventHorizon.track(
                    NewsletterOptInChangedEvent(
                        source = NewsletterSourceType.WelcomeNewAccount,
                        enabled = state.subscribedToNewsletter,
                    ),
                )
                settings.marketingOptIn.set(state.subscribedToNewsletter, updateModifiedAt = true)
                viewModelScope.launch {
                    _messagesFlow.emit(
                        if (state.notificationsEnabled) {
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
        eventHorizon.track(NotificationsPermissionsShownEvent)
    }

    fun reportNotificationsOptInShown() {
        eventHorizon.track(NotificationsOptInShownEvent)
    }

    fun reportNotificationRequestResult(wasEnabled: Boolean) {
        val event = if (wasEnabled) {
            NotificationsOptInAllowedEvent
        } else {
            NotificationsOptInDeniedEvent
        }
        eventHorizon.track(event)
    }

    fun handleDismissedByUser() {
        settings.notificationsPromptAcknowledged.set(value = true, updateModifiedAt = true)
        eventHorizon.track(NotificationsPermissionsNotNowTappedEvent)
    }

    fun changeNewsletterSubscription(isSubscribed: Boolean) {
        _stateFlow.update {
            (it as? UiState.NewOnboarding)?.copy(subscribedToNewsletter = isSubscribed) ?: it
        }
    }

    fun changeNotificationsEnabled(areEnabled: Boolean) {
        _stateFlow.update {
            (it as? UiState.NewOnboarding)?.copy(notificationsEnabled = areEnabled) ?: it
        }
    }

    sealed interface UiState {
        object PreNewOnboarding : UiState
        data class NewOnboarding(
            val showNewsletterOptIn: Boolean,
            val subscribedToNewsletter: Boolean,
            val notificationsEnabled: Boolean,
        ) : UiState
    }

    sealed interface UiMessage {
        object RequestPermission : UiMessage
        object Dismiss : UiMessage
    }
}
