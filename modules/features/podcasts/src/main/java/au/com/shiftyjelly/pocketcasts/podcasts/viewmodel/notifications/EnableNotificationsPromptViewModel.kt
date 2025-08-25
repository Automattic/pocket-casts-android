package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.viewmodel.NewsletterSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
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
    private val analyticsTracker: AnalyticsTracker,
    private val userManager: UserManager,
) : ViewModel() {

    private var _stateFlow: MutableStateFlow<UiState> = MutableStateFlow(UiState.PreNewOnboarding)
    val stateFlow: StateFlow<UiState> = _stateFlow

    private var _messagesFlow: MutableSharedFlow<UiMessage> = MutableSharedFlow()
    val messagesFlow: SharedFlow<UiMessage> = _messagesFlow

    init {
        if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION)) {
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
    }

    fun handleCtaClick() {
        analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_PERMISSIONS_ALLOW_TAPPED)
        when (val state = stateFlow.value) {
            is UiState.PreNewOnboarding -> {
                viewModelScope.launch {
                    _messagesFlow.emit(UiMessage.RequestPermission)
                }
            }

            is UiState.NewOnboarding -> {
                analyticsTracker.track(
                    AnalyticsEvent.NEWSLETTER_OPT_IN_CHANGED,
                    mapOf(
                        "source" to NewsletterSource.WELCOME_NEW_ACCOUNT.analyticsValue,
                        "enabled" to state.subscribedToNewsletter,
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
