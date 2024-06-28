package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveLimit
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = PodcastAutoArchiveViewModel.Factory::class)
class PodcastAutoArchiveViewModel @AssistedInject constructor(
    @Assisted private val podcastUuid: String,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {
    val state = combine(
        podcastManager.observePodcastByUuidFlow(podcastUuid),
        settings.autoArchiveAfterPlaying.flow,
        settings.autoArchiveInactive.flow,
    ) { podcast, archiveAfterPlaying, archiveInactive ->
        State(
            overrideAutoArchiveSettings = podcast.overrideGlobalArchive,
            archiveAfterPlaying = podcast.autoArchiveAfterPlaying ?: archiveAfterPlaying,
            archiveInactive = podcast.autoArchiveInactive ?: archiveInactive,
            episodeLimit = podcast.autoArchiveEpisodeLimit ?: AutoArchiveLimit.None,
            podcast = podcast,
        )
    }.shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

    fun updateGlobalOverride(checked: Boolean) {
        viewModelScope.launch {
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_ARCHIVE_TOGGLED,
                mapOf("enabled" to checked),
            )
            podcastManager.updateArchiveSettings(
                podcastUuid,
                checked,
                settings.autoArchiveAfterPlaying.value,
                settings.autoArchiveInactive.value,
            )
        }
    }

    fun updateAfterPlaying(value: AutoArchiveAfterPlaying) {
        viewModelScope.launch {
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_ARCHIVE_PLAYED_CHANGED,
                mapOf("value" to value.analyticsValue),
            )
            podcastManager.updateArchiveAfterPlaying(podcastUuid, value)
        }
    }

    fun updateInactive(value: AutoArchiveInactive) {
        viewModelScope.launch {
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_ARCHIVE_INACTIVE_CHANGED,
                mapOf("value" to value.analyticsValue),
            )
            podcastManager.updateArchiveAfterInactive(podcastUuid, value)
        }
    }

    fun updateEpisodeLimit(value: AutoArchiveLimit) {
        viewModelScope.launch {
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_ARCHIVE_EPISODE_LIMIT_CHANGED,
                mapOf("value" to value.analyticsValue),
            )
            podcastManager.updateArchiveEpisodeLimit(podcastUuid, value)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(podcastUuid: String): PodcastAutoArchiveViewModel
    }

    data class State(
        val overrideAutoArchiveSettings: Boolean = false,
        val archiveAfterPlaying: AutoArchiveAfterPlaying = AutoArchiveAfterPlaying.Never,
        val archiveInactive: AutoArchiveInactive = AutoArchiveInactive.Never,
        val episodeLimit: AutoArchiveLimit = AutoArchiveLimit.None,
        val podcast: Podcast? = null,
    )
}
