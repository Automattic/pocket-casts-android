package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.AutoDownloadLimitSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.AutoDownloadSettingsRoute
import au.com.shiftyjelly.pocketcasts.utils.extensions.combine
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
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
    private val downloadManager: DownloadManager,
    private val settings: Settings,
    private val tracker: AnalyticsTracker,
) : ViewModel() {
    private val podcastsFlow = MutableStateFlow<List<Podcast>?>(null)
    private val playlistsFlow = MutableStateFlow<List<PlaylistPreview>?>(null)

    init {
        viewModelScope.launch {
            podcastsFlow.value = podcastManager.findSubscribedFlow().first()
        }
        viewModelScope.launch {
            playlistsFlow.value = if (FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)) {
                playlistManager.playlistPreviewsFlow()
            } else {
                playlistManager.playlistPreviewsFlow().map { playlists -> playlists.filterIsInstance<SmartPlaylistPreview>() }
            }.first()
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
        tracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_UP_NEXT_TOGGLED,
            mapOf("enabled" to enable),
        )

        settings.autoDownloadUpNext.set(enable, updateModifiedAt = true)
    }

    fun changeNewEpisodesDownload(enable: Boolean) {
        tracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_NEW_EPISODES_TOGGLED,
            mapOf("enabled" to enable),
        )

        val newValue = if (enable) Podcast.AUTO_DOWNLOAD_NEW_EPISODES else Podcast.AUTO_DOWNLOAD_OFF
        settings.autoDownloadNewEpisodes.set(newValue, updateModifiedAt = true)
    }

    fun changeOnFollowDownload(enable: Boolean) {
        tracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_ON_FOLLOW_PODCAST_TOGGLED,
            mapOf("enabled" to enable),
        )

        settings.autoDownloadOnFollowPodcast.set(enable, updateModifiedAt = true)
    }

    fun changePodcastDownloadLimit(limit: AutoDownloadLimitSetting) {
        tracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_LIMIT_DOWNLOADS_CHANGED,
            mapOf("value" to limit.episodeCount),
        )

        settings.autoDownloadLimit.set(limit, updateModifiedAt = true)
    }

    fun changeOnUnmeteredDownload(enable: Boolean) {
        tracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_ONLY_ON_WIFI_TOGGLED,
            mapOf("enabled" to enable),
        )

        settings.autoDownloadUnmeteredOnly.set(enable, updateModifiedAt = true)
    }

    fun changeOnlyWhenChargingDownload(enable: Boolean) {
        tracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_ONLY_WHEN_CHARGING_TOGGLED,
            mapOf("enabled" to enable),
        )

        settings.autoDownloadOnlyWhenCharging.set(enable, updateModifiedAt = true)
    }

    fun changePodcastAutoDownload(podcastUuid: String, enable: Boolean) {
        tracker.track(
            AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_PODCASTS_CHANGED,
            mapOf("source" to "downloads"),
        )
        tracker.track(
            AnalyticsEvent.SETTINGS_SELECT_PODCASTS_PODCAST_TOGGLED,
            mapOf(
                "source" to "downloads",
                "uuid" to podcastUuid,
                "enabled" to enable,
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
        tracker.track(
            if (enable) {
                AnalyticsEvent.SETTINGS_SELECT_PODCASTS_SELECT_ALL_TAPPED
            } else {
                AnalyticsEvent.SETTINGS_SELECT_PODCASTS_SELECT_NONE_TAPPED
            },
            mapOf("source" to "downloads"),
        )

        viewModelScope.launch {
            val podcastUuids = withContext(Dispatchers.Default) {
                updatePodcastAutoDownloadStatus(enable)
                podcastsFlow.value?.map(Podcast::uuid)
            }
            if (podcastUuids != null) {
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
        tracker.track(AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_FILTERS_CHANGED)

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
        tracker.track(AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_STOP_ALL_DOWNLOADS)

        downloadManager.stopAllDownloads()
    }

    fun clearDownloadErrors() {
        tracker.track(AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_CLEAR_DOWNLOAD_ERRORS)

        viewModelScope.launch(Dispatchers.IO) {
            podcastManager.clearAllDownloadErrorsBlocking()
        }
    }

    internal fun trackPageShown(route: AutoDownloadSettingsRoute) {
        when (route) {
            AutoDownloadSettingsRoute.Home -> {
                tracker.track(AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_SHOWN)
            }

            AutoDownloadSettingsRoute.Podcasts -> {
                tracker.track(
                    AnalyticsEvent.SETTINGS_SELECT_PODCASTS_SHOWN,
                    mapOf("source" to "downloads"),
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
