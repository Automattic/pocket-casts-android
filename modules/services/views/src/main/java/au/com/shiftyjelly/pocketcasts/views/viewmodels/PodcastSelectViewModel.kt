package au.com.shiftyjelly.pocketcasts.views.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragmentSource
import au.com.shiftyjelly.pocketcasts.views.fragments.SelectablePodcast
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SettingsAutoAddUpNextPodcastsChangedEvent
import com.automattic.eventhorizon.SettingsNotificationsPodcastsChangedEvent
import com.automattic.eventhorizon.SettingsSelectPodcastsDismissedEvent
import com.automattic.eventhorizon.SettingsSelectPodcastsPodcastToggledEvent
import com.automattic.eventhorizon.SettingsSelectPodcastsSelectAllTappedEvent
import com.automattic.eventhorizon.SettingsSelectPodcastsSelectNoneTappedEvent
import com.automattic.eventhorizon.SettingsSelectPodcastsShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PodcastSelectViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    private val _selectablePodcasts = MutableStateFlow<List<SelectablePodcast>>(emptyList())
    val selectablePodcasts: StateFlow<List<SelectablePodcast>> = _selectablePodcasts

    fun loadSelectablePodcasts(selectedUuids: List<String>) {
        viewModelScope.launch {
            val podcasts = withContext(Dispatchers.IO) {
                podcastManager.findSubscribedBlocking()
            }

            val sortedSelectable = podcasts
                .sortedBy { PodcastsSortType.cleanStringForSort(it.title) }
                .map { SelectablePodcast(it, selectedUuids.contains(it.uuid)) }

            _selectablePodcasts.value = sortedSelectable
        }
    }

    fun trackOnShown(source: PodcastSelectFragmentSource) {
        eventHorizon.track(
            SettingsSelectPodcastsShownEvent(
                source = source.eventHorizonValue,
            ),
        )
    }

    fun trackOnPodcastToggled(source: PodcastSelectFragmentSource, podcastUuid: String, enabled: Boolean) {
        eventHorizon.track(
            SettingsSelectPodcastsPodcastToggledEvent(
                uuid = podcastUuid,
                enabled = enabled,
                source = source.eventHorizonValue,
            ),
        )
    }

    fun trackOnSelectNoneTapped(source: PodcastSelectFragmentSource) {
        eventHorizon.track(
            SettingsSelectPodcastsSelectNoneTappedEvent(
                source = source.eventHorizonValue,
            ),
        )
    }

    fun trackOnSelectAllTapped(source: PodcastSelectFragmentSource) {
        eventHorizon.track(
            SettingsSelectPodcastsSelectAllTappedEvent(
                source = source.eventHorizonValue,
            ),
        )
    }

    fun trackOnDismissed(source: PodcastSelectFragmentSource) {
        eventHorizon.track(
            SettingsSelectPodcastsDismissedEvent(
                source = source.eventHorizonValue,
            ),
        )
    }

    fun trackAutoAddPodcastsChanged(count: Int) {
        eventHorizon.track(
            SettingsAutoAddUpNextPodcastsChangedEvent(
                numberSelected = count.toLong(),
            ),
        )
    }

    fun trackNotificationsChanged(count: Int) {
        eventHorizon.track(
            SettingsNotificationsPodcastsChangedEvent(
                numberSelected = count.toLong(),
            ),
        )
    }
}
