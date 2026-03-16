package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.flow.combine
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.AutoDownloadLimitSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.AutoDownloadSettingsRoute
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SelectPodcastsSource
import com.automattic.eventhorizon.SettingsAutoDownloadClearDownloadErrorsEvent
import com.automattic.eventhorizon.SettingsAutoDownloadFiltersChangedEvent
import com.automattic.eventhorizon.SettingsAutoDownloadLimitDownloadsChangedEvent
import com.automattic.eventhorizon.SettingsAutoDownloadNewEpisodesToggledEvent
import com.automattic.eventhorizon.SettingsAutoDownloadOnFollowPodcastToggledEvent
import com.automattic.eventhorizon.SettingsAutoDownloadOnlyOnWifiToggledEvent
import com.automattic.eventhorizon.SettingsAutoDownloadOnlyWhenChargingToggledEvent
import com.automattic.eventhorizon.SettingsAutoDownloadPodcastsChangedEvent
import com.automattic.eventhorizon.SettingsAutoDownloadShownEvent
import com.automattic.eventhorizon.SettingsAutoDownloadStopAllDownloadsEvent
import com.automattic.eventhorizon.SettingsAutoDownloadUpNextToggledEvent
import com.automattic.eventhorizon.SettingsSelectPodcastsPodcastToggledEvent
import com.automattic.eventhorizon.SettingsSelectPodcastsSelectAllTappedEvent
import com.automattic.eventhorizon.SettingsSelectPodcastsSelectNoneTappedEvent
import com.automattic.eventhorizon.SettingsSelectPodcastsShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AutoDownloadSettingsViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val playlistManager: PlaylistManager,
    private val episodeManager: EpisodeManager,
    private val downloadQueue: DownloadQueue,
    private val settings: Settings,
    private val eventHorizon: EventHorizon,
) : ViewModel() {
    private val podcastsFlow = MutableStateFlow<List<Podcast>?>(null)
    private val playlistsFlow = MutableStateFlow<List<PlaylistPreview>?>(null)

    init {
        viewModelScope.launch {
            podcastsFlow.value = podcastManager.findSubscribedFlow().first()
        }
        viewModelScope.launch {
            playlistsFlow.value = playlistManager.playlistPreviewsFlow().first()
        }
    }

    val uiState = combine(
        settings.autoDownloadUpNext.flow,
        settings.autoDownloadNewEpisodes.flow.map { setting -> setting == Podcast.AUTO_DOWNLOAD_NEW_EPISODES },
        settings.autoDownloadOnFollowPodcast.flow,
        settings.autoDownloadLimit.flow,
        settings.autoDownloadUnmeteredOnly.flow,
        settings.autoDownloadOnlyWhenCharging.flow,
        podcastsFlow,
        playlistsFlow,
        transform = { upNext, newEpisodes, onFollow, limit, unmetered, whenCharging, podcasts, playlists ->
            if (podcasts != null && playlists != null) {
                UiState(
                    isUpNextDownloadEnabled = upNext,
                    isNewEpisodesDownloadEnabled = newEpisodes,
                    isOnFollowDownloadEnabled = onFollow,
                    autoDownloadLimit = limit,
                    isOnUnmeteredDownloadEnabled = unmetered,
                    isOnlyWhenChargingDownloadEnabled = whenCharging,
                    podcasts = podcasts,
                    playlists = playlists,
                )
            } else {
                null
            }
        },
    ).stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    fun getArtworkUuidsFlow(playlistUuid: String): StateFlow<List<String>?> {
        return playlistManager.getArtworkUuidsFlow(playlistUuid)
    }

    suspend fun refreshArtworkUuids(playlistUuid: String) {
        playlistManager.refreshArtworkUuids(playlistUuid)
    }

    fun changeUpNextDownload(enable: Boolean) {
        eventHorizon.track(
            SettingsAutoDownloadUpNextToggledEvent(
                enabled = enable,
            ),
        )

        settings.autoDownloadUpNext.set(enable, updateModifiedAt = true)
    }

    fun changeNewEpisodesDownload(enable: Boolean) {
        eventHorizon.track(
            SettingsAutoDownloadNewEpisodesToggledEvent(
                enabled = enable,
            ),
        )

        val newValue = if (enable) Podcast.AUTO_DOWNLOAD_NEW_EPISODES else Podcast.AUTO_DOWNLOAD_OFF
        settings.autoDownloadNewEpisodes.set(newValue, updateModifiedAt = true)
    }

    fun changeOnFollowDownload(enable: Boolean) {
        eventHorizon.track(
            SettingsAutoDownloadOnFollowPodcastToggledEvent(
                enabled = enable,
            ),
        )

        settings.autoDownloadOnFollowPodcast.set(enable, updateModifiedAt = true)
    }

    fun changePodcastDownloadLimit(limit: AutoDownloadLimitSetting) {
        eventHorizon.track(
            SettingsAutoDownloadLimitDownloadsChangedEvent(
                value = limit.episodeCount.toLong(),
            ),
        )

        settings.autoDownloadLimit.set(limit, updateModifiedAt = true)
    }

    fun changeOnUnmeteredDownload(enable: Boolean) {
        eventHorizon.track(
            SettingsAutoDownloadOnlyOnWifiToggledEvent(
                enabled = enable,
            ),
        )

        settings.autoDownloadUnmeteredOnly.set(enable, updateModifiedAt = true)
    }

    fun changeOnlyWhenChargingDownload(enable: Boolean) {
        eventHorizon.track(
            SettingsAutoDownloadOnlyWhenChargingToggledEvent(
                enabled = enable,
            ),
        )

        settings.autoDownloadOnlyWhenCharging.set(enable, updateModifiedAt = true)
    }

    fun changePodcastAutoDownload(podcastUuid: String, enable: Boolean) {
        eventHorizon.track(SettingsAutoDownloadPodcastsChangedEvent)
        eventHorizon.track(
            SettingsSelectPodcastsPodcastToggledEvent(
                uuid = podcastUuid,
                enabled = enable,
                source = SelectPodcastsSource.Downloads,
            ),
        )

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                updatePodcastAutoDownloadStatus(enable) { podcast -> podcast.uuid == podcastUuid }
            }

            podcastManager.updateAutoDownload(listOf(podcastUuid), isEnabled = enable)
        }
    }

    fun changeAllPodcastsAutoDownload(enable: Boolean) {
        val event = if (enable) {
            SettingsSelectPodcastsSelectAllTappedEvent(
                source = SelectPodcastsSource.Downloads,
            )
        } else {
            SettingsSelectPodcastsSelectNoneTappedEvent(
                source = SelectPodcastsSource.Downloads,
            )
        }
        eventHorizon.track(event)

        viewModelScope.launch {
            val podcastUuids = withContext(Dispatchers.Default) {
                updatePodcastAutoDownloadStatus(enable)
                podcastsFlow.value?.map(Podcast::uuid)
            }
            if (!podcastUuids.isNullOrEmpty()) {
                podcastManager.updateAutoDownload(podcastUuids, isEnabled = enable)
            }
        }
    }

    private fun updatePodcastAutoDownloadStatus(
        enable: Boolean,
        predicate: (Podcast) -> Boolean = { true },
    ) {
        val status = if (enable) Podcast.AUTO_DOWNLOAD_NEW_EPISODES else Podcast.AUTO_DOWNLOAD_OFF
        podcastsFlow.update { podcasts ->
            podcasts?.map { podcast ->
                if (predicate(podcast)) {
                    podcast.copy(autoDownloadStatus = status)
                } else {
                    podcast
                }
            }
        }
    }

    fun changePlaylistAutoDownload(playlistUuid: String, enable: Boolean) {
        eventHorizon.track(SettingsAutoDownloadFiltersChangedEvent)

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                updatePlaylistsAutoDownloadUiState(enable) { playlist -> playlist.uuid == playlistUuid }
            }

            playlistManager.updateAutoDownload(playlistUuid, isEnabled = enable)
        }
    }

    fun changeAllPlaylistsAutoDownload(enable: Boolean) {
        viewModelScope.launch {
            val playlistUuids = withContext(Dispatchers.Default) {
                updatePlaylistsAutoDownloadUiState(enable)
                playlistsFlow.value?.map(PlaylistPreview::uuid)
            }
            if (playlistUuids != null) {
                playlistManager.updateAutoDownload(playlistUuids, isEnabled = enable)
            }
        }
    }

    private fun updatePlaylistsAutoDownloadUiState(
        enable: Boolean,
        predicate: (PlaylistPreview) -> Boolean = { true },
    ) {
        playlistsFlow.update { playlists ->
            playlists?.map { playlist ->
                if (predicate(playlist)) {
                    val newSettings = playlist.settings.copy(isAutoDownloadEnabled = enable)
                    when (playlist) {
                        is ManualPlaylistPreview -> playlist.copy(settings = newSettings)
                        is SmartPlaylistPreview -> playlist.copy(settings = newSettings)
                    }
                } else {
                    playlist
                }
            }
        }
    }

    fun stopAllDownloads() {
        eventHorizon.track(SettingsAutoDownloadStopAllDownloadsEvent)
        viewModelScope.launch {
            val cancelledDownloads = downloadQueue.cancelAll(SourceView.DOWNLOADS).await()
            episodeManager.disableAutoDownload(cancelledDownloads)
        }
    }

    fun clearDownloadErrors() {
        eventHorizon.track(SettingsAutoDownloadClearDownloadErrorsEvent)

        viewModelScope.launch(Dispatchers.IO) {
            podcastManager.clearAllDownloadErrorsBlocking()
        }
    }

    internal fun trackPageShown(route: AutoDownloadSettingsRoute) {
        when (route) {
            AutoDownloadSettingsRoute.Home -> {
                eventHorizon.track(SettingsAutoDownloadShownEvent)
            }

            AutoDownloadSettingsRoute.Podcasts -> {
                eventHorizon.track(
                    SettingsSelectPodcastsShownEvent(
                        source = SelectPodcastsSource.Downloads,
                    ),
                )
            }

            // No tracking event
            AutoDownloadSettingsRoute.Playlists -> Unit
        }
    }

    data class UiState(
        val isUpNextDownloadEnabled: Boolean,
        val isNewEpisodesDownloadEnabled: Boolean,
        val isOnFollowDownloadEnabled: Boolean,
        val autoDownloadLimit: AutoDownloadLimitSetting,
        val isOnUnmeteredDownloadEnabled: Boolean,
        val isOnlyWhenChargingDownloadEnabled: Boolean,
        val podcasts: List<Podcast>,
        val playlists: List<PlaylistPreview>,
    )
}
