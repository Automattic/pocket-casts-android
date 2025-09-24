package au.com.shiftyjelly.pocketcasts.views.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragmentSource
import au.com.shiftyjelly.pocketcasts.views.fragments.SelectablePodcast
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel
class PodcastSelectViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val analyticsTracker: AnalyticsTracker,
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

    fun trackChange(source: PodcastSelectFragmentSource?, props: Map<String, Int>) {
        when (source) {
            PodcastSelectFragmentSource.AUTO_ADD -> {
                analyticsTracker.track(AnalyticsEvent.SETTINGS_AUTO_ADD_UP_NEXT_PODCASTS_CHANGED, props)
            }

            PodcastSelectFragmentSource.NOTIFICATIONS -> {
                analyticsTracker.track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_PODCASTS_CHANGED, props)
            }

            PodcastSelectFragmentSource.FILTERS -> {
                // Do not track because the filter_updated event was tracked when the change was persisted
            }

            null -> {
                Timber.e("No source set for ${this::class.java.simpleName}")
            }
        }
    }

    private fun PodcastSelectFragmentSource?.toEventProperty(): Map<String, String> {
        return this?.analyticsValue?.let { mapOf("source" to it) } ?: emptyMap()
    }
}
