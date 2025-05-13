package au.com.shiftyjelly.pocketcasts.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.settings.notifications.data.NotificationsPreferenceRepository
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
internal class NotificationsSettingViewModel @Inject constructor(
    private val preferenceRepository: NotificationsPreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(State(emptyList()))
    val state: StateFlow<State> = _state

    init {
        loadPreferences()
    }

    internal fun loadPreferences() = viewModelScope.launch {
        _state.update { it.copy(categories = preferenceRepository.getPreferenceCategories()) }
    }

    internal fun onPreferenceClicked(preference: NotificationPreference) {
        // To be implemented later
    }

    internal data class State(
        val categories: List<NotificationPreferenceCategory>
    )
}