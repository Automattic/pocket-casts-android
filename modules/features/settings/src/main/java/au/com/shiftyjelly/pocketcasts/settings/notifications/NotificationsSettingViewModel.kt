package au.com.shiftyjelly.pocketcasts.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.settings.notifications.data.NotificationsPreferenceRepository
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceCategory
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
internal class NotificationsSettingViewModel @Inject constructor(
    private val preferenceRepository: NotificationsPreferenceRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _state = MutableStateFlow(State(emptyList()))
    val state: StateFlow<State> = _state

    init {
        loadPreferences()
    }

    internal fun loadPreferences() = viewModelScope.launch {
        _state.update { it.copy(categories = preferenceRepository.getPreferenceCategories()) }
    }

    internal fun onPreferenceClicked(preference: NotificationPreference<*>) {
        when (preference.preference) {
            NotificationPreferences.NEW_EPISODES_NOTIFY_ME -> {
                val newValue = (preference as? NotificationPreference.SwitchPreference)?.value?.not() ?: error("Invalid value for notify me on new episode preference: ${preference.value}")
                viewModelScope.launch {
                    preferenceRepository.setPreference(preference.copy(value = newValue))
                    analyticsTracker.track(
                        AnalyticsEvent.SETTINGS_NOTIFICATIONS_NEW_EPISODES_TOGGLED,
                        mapOf("enabled" to newValue),
                    )
                    loadPreferences()
                }
            }
            NotificationPreferences.SETTINGS_HIDE_NOTIFICATION_ON_PAUSE -> {
                viewModelScope.launch {
                    val newValue = (preference as? NotificationPreference.SwitchPreference)?.value?.not() ?: error("Invalid value for hide notification on pause preference: ${preference.value}")
                    preferenceRepository.setPreference(preference.copy(value = newValue))
                    analyticsTracker.track(
                        AnalyticsEvent.SETTINGS_NOTIFICATIONS_HIDE_PLAYBACK_NOTIFICATION_ON_PAUSE,
                        mapOf("enabled" to newValue),
                    )
                    loadPreferences()
                }
            }
            else -> Unit // TO BE IMPLEMENTED LATER
        }
    }

    internal fun onShown() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_SHOWN)
    }

    internal data class State(
        val categories: List<NotificationPreferenceCategory>
    )
}