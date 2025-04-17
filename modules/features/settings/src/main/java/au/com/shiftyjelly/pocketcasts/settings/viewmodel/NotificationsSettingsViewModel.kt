package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@HiltViewModel
class NotificationsSettingsViewModel @Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
    private val podcastManager: PodcastManager,
) : ViewModel() {

    private val mutableState = MutableStateFlow(initState())
    val state: StateFlow<NotificationSettingsState> = mutableState

    fun onPodcastsSelectionChanged(newSelection: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            podcastManager.findSubscribedBlocking().forEach {
                podcastManager.updateShowNotifications(it.uuid, newSelection.contains(it.uuid))
            }
        }
    }

    fun getCurrentPodcastsSelection(): List<String> {
        return runBlocking {
            async(Dispatchers.Default) {
                val uuids = podcastManager.findSubscribedBlocking().filter { it.isShowNotifications }.map { it.uuid }
                uuids
            }.await()
        }
    }

    private fun initState() = NotificationSettingsState(
        newEpisodesState = NotificationSettingsState.NewEpisodesState(
            isChecked = settings.notifyRefreshPodcast.flow.value,
            onCheckedChange = { enabled ->
                onNewEpisodesCheckedChange(enabled)
            },
        ),
    )

    private fun onNewEpisodesCheckedChange(enabled: Boolean) {
        mutableState.value = mutableState.value.copy(
            newEpisodesState = mutableState.value.newEpisodesState.copy(
                isChecked = enabled,
            ),
        )
        settings.notifyRefreshPodcast.set(enabled, updateModifiedAt = true)

        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_NOTIFICATIONS_NEW_EPISODES_TOGGLED,
            mapOf("enabled" to enabled),
        )

        if (enabled) {
            settings.setNotificationLastSeenToNow()
        }
    }

    data class NotificationSettingsState(
        val newEpisodesState: NewEpisodesState,
    ) {
        data class NewEpisodesState(
            val isChecked: Boolean,
            val onCheckedChange: (Boolean) -> Unit,
        )
    }
}
