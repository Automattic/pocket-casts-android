package au.com.shiftyjelly.pocketcasts.views.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragmentSource
import au.com.shiftyjelly.pocketcasts.views.fragments.SelectablePodcast
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SettingsAutoAddUpNextPodcastsChangedEvent
import com.automattic.eventhorizon.SettingsNotificationsPodcastsChangedEvent
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
    private val analyticsTracker: AnalyticsTracker,
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
        analyticsTracker.track(AnalyticsEvent.SETTINGS_SELECT_PODCASTS_SHOWN, source.toEventProperty())
    }

    fun trackOnPodcastToggled(source: PodcastSelectFragmentSource, podcastUuid: String, enabled: Boolean) {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_SELECT_PODCASTS_PODCAST_TOGGLED, source.toEventProperty() + mapOf("uuid" to podcastUuid, "enabled" to enabled))
    }

    fun trackOnSelectNoneTapped(source: PodcastSelectFragmentSource) {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_SELECT_PODCASTS_SELECT_NONE_TAPPED, source.toEventProperty())
    }

    fun trackOnSelectAllTapped(source: PodcastSelectFragmentSource) {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_SELECT_PODCASTS_SELECT_ALL_TAPPED, source.toEventProperty())
    }

    fun trackOnDismissed(source: PodcastSelectFragmentSource) {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_SELECT_PODCASTS_DISMISSED, source.toEventProperty())
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

    private fun PodcastSelectFragmentSource?.toEventProperty(): Map<String, String> {
        return this?.analyticsValue?.let { mapOf("source" to it) } ?: emptyMap()
    }
}
