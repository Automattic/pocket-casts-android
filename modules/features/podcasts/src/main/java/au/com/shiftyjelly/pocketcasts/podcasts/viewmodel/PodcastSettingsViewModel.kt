package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveLimit
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.decrementByOrRound
import au.com.shiftyjelly.pocketcasts.utils.extensions.incrementByOrRound
import au.com.shiftyjelly.pocketcasts.utils.extensions.roundedSpeed
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel(assistedFactory = PodcastSettingsViewModel.Factory::class)
class PodcastSettingsViewModel @AssistedInject constructor(
    private val podcastManager: PodcastManager,
    private val playlistManager: PlaylistManager,
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
    private val tracker: AnalyticsTracker,
    @Assisted private val podcastUuid: String,
) : ViewModel() {
    private val playlistsWithSelectedPodcasts = playlistManager.playlistPreviewsFlow()
        .map { playlists ->
            playlists
                .filterIsInstance<SmartPlaylistPreview>()
                .filter { playlist ->
                    when (playlist.smartRules.podcasts) {
                        is PodcastsRule.Any -> false
                        is PodcastsRule.Selected -> true
                    }
                }
        }

    val uiState = combine(
        podcastManager.podcastByUuidFlow(podcastUuid),
        playlistsWithSelectedPodcasts,
        settings.autoAddUpNextLimit.flow,
        ::UiState,
    ).stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    fun changeNotifications(enable: Boolean) {
        viewModelScope.launch {
            tracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_NOTIFICATIONS_TOGGLED,
                mapOf("enabled" to enable),
            )
            podcastManager.updateShowNotifications(podcastUuid, show = enable)
        }
    }

    fun changeAutoDownload(enable: Boolean) {
        val podcast = uiState.value?.podcast ?: return
        viewModelScope.launch(Dispatchers.IO) {
            tracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_DOWNLOAD_TOGGLED,
                mapOf("enabled" to enable),
            )
            val status = if (enable) Podcast.AUTO_DOWNLOAD_NEW_EPISODES else Podcast.AUTO_DOWNLOAD_OFF
            podcastManager.updateAutoDownloadStatusBlocking(podcast, status)
        }
    }

    fun changeAddToUpNext(enable: Boolean) {
        val podcast = uiState.value?.podcast ?: return
        val mode = if (enable) Podcast.AutoAddUpNext.PLAY_LAST else Podcast.AutoAddUpNext.OFF
        viewModelScope.launch {
            tracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_ADD_UP_NEXT_TOGGLED,
                mapOf("enabled" to enable),
            )
            podcastManager.updateAutoAddToUpNext(podcast, mode)
        }
    }

    fun changeAddToUpNext(mode: Podcast.AutoAddUpNext) {
        val podcast = uiState.value?.podcast ?: return
        viewModelScope.launch {
            tracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_ADD_UP_NEXT_POSITION_OPTION_CHANGED,
                mapOf("value" to mode.analyticsValue),
            )
            podcastManager.updateAutoAddToUpNext(podcast, mode)
        }
    }

    fun changeAutoArchive(enable: Boolean) {
        viewModelScope.launch {
            tracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_ARCHIVE_TOGGLED,
                mapOf("enabled" to enable),
            )

            podcastManager.updateArchiveSettings(
                uuid = podcastUuid,
                enable = enable,
                afterPlaying = settings.autoArchiveAfterPlaying.value,
                inactive = settings.autoArchiveInactive.value,
            )
        }
    }

    fun changeAutoArchiveAfterPlaying(mode: AutoArchiveAfterPlaying) {
        viewModelScope.launch {
            tracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_ARCHIVE_PLAYED_CHANGED,
                mapOf("value" to mode.analyticsValue),
            )
            podcastManager.updateArchiveAfterPlaying(podcastUuid, mode)
        }
    }

    fun changeAutoArchiveAfterInactive(mode: AutoArchiveInactive) {
        viewModelScope.launch {
            tracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_ARCHIVE_INACTIVE_CHANGED,
                mapOf("value" to mode.analyticsValue),
            )
            podcastManager.updateArchiveAfterInactive(podcastUuid, mode)
        }
    }

    fun changeAutoArchiveLimit(limit: AutoArchiveLimit) {
        viewModelScope.launch {
            tracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_ARCHIVE_EPISODE_LIMIT_CHANGED,
                mapOf("value" to limit.analyticsValue),
            )
            podcastManager.updateArchiveEpisodeLimit(podcastUuid, limit)
        }
    }

    fun changePlaybackEffects(enable: Boolean) {
        val podcast = uiState.value?.podcast ?: return
        viewModelScope.launch(Dispatchers.IO) {
            tracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_CUSTOM_PLAYBACK_EFFECTS_TOGGLED,
                mapOf(
                    "enabled" to enable,
                    "settings" to "local",
                ),
            )
            podcastManager.updateOverrideGlobalEffectsBlocking(podcast, enable)
        }
    }

    fun decrementPlaybackSpeed() {
        val podcast = uiState.value?.podcast ?: return
        changePlaybackSpeed(podcast, change = -0.1)
    }

    fun incrementPlaybackSpeed() {
        val podcast = uiState.value?.podcast ?: return
        changePlaybackSpeed(podcast, change = 0.1)
    }

    private fun changePlaybackSpeed(podcast: Podcast, change: Double) {
        val newPlaybackSpeed = (podcast.playbackSpeed + change).roundedSpeed()
        viewModelScope.launch(Dispatchers.IO) {
            tracker.track(
                AnalyticsEvent.PLAYBACK_EFFECT_SPEED_CHANGED,
                mapOf(
                    "speed" to newPlaybackSpeed,
                    "settings" to "local",
                ),
            )

            podcastManager.updatePlaybackSpeedBlocking(podcast, newPlaybackSpeed)

            val newEffects = podcast.playbackEffects.toData()
                .copy(playbackSpeed = newPlaybackSpeed)
                .toEffects()
            updatePlayerEffects(newEffects)
        }
    }

    fun changeTrimMode(enable: Boolean) {
        val podcast = uiState.value?.podcast ?: return
        val mode = if (enable) TrimMode.LOW else TrimMode.OFF
        viewModelScope.launch(Dispatchers.IO) {
            tracker.track(
                AnalyticsEvent.PLAYBACK_EFFECT_TRIM_SILENCE_TOGGLED,
                mapOf(
                    "enabled" to enable,
                    "settings" to "local",
                ),
            )

            podcastManager.updateTrimModeBlocking(podcast, mode)

            val newEffects = podcast.playbackEffects.toData()
                .copy(trimMode = mode)
                .toEffects()
            updatePlayerEffects(newEffects)
        }
    }

    fun changeTrimMode(mode: TrimMode) {
        val podcast = uiState.value?.podcast ?: return
        viewModelScope.launch(Dispatchers.IO) {
            tracker.track(
                AnalyticsEvent.PLAYBACK_EFFECT_TRIM_SILENCE_TOGGLED,
                mapOf(
                    "amount" to mode.analyticsVale,
                    "settings" to "local",
                ),
            )

            podcastManager.updateTrimModeBlocking(podcast, mode)

            val newEffects = podcast.playbackEffects.toData()
                .copy(trimMode = mode)
                .toEffects()
            updatePlayerEffects(newEffects)
        }
    }

    fun changeVolumeBoost(enable: Boolean) {
        val podcast = uiState.value?.podcast ?: return
        viewModelScope.launch(Dispatchers.IO) {
            tracker.track(
                AnalyticsEvent.PLAYBACK_EFFECT_VOLUME_BOOST_TOGGLED,
                mapOf(
                    "enabled" to enable,
                    "settings" to "local",
                ),
            )

            podcastManager.updateVolumeBoostedBlocking(podcast, enable)

            val newEffects = podcast.playbackEffects.toData()
                .copy(isVolumeBoosted = enable)
                .toEffects()
            updatePlayerEffects(newEffects)
        }
    }

    fun decrementSkipFirst() {
        changeSkipFirst { value -> value.decrementByOrRound(5).coerceAtLeast(0) }
    }

    fun incrementSkipFirst() {
        changeSkipFirst { value -> value.incrementByOrRound(5).coerceAtLeast(0) }
    }

    private fun changeSkipFirst(block: (Int) -> Int) {
        val podcast = uiState.value?.podcast ?: return
        viewModelScope.launch {
            val newValue = block(podcast.startFromSecs)
            tracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_SKIP_FIRST_CHANGED,
                mapOf("value" to newValue),
            )
            podcastManager.updateStartFromInSec(podcast, newValue)
        }
    }

    fun decrementSkipLast() {
        changeSkipLast { value -> value.decrementByOrRound(5).coerceAtLeast(0) }
    }

    fun incrementSkipLast() {
        changeSkipLast { value -> value.incrementByOrRound(5).coerceAtLeast(0) }
    }

    private fun changeSkipLast(block: (Int) -> Int) {
        val podcast = uiState.value?.podcast ?: return
        viewModelScope.launch {
            val newValue = block(podcast.skipLastSecs)
            tracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_SKIP_LAST_CHANGED,
                mapOf("value" to newValue),
            )
            podcastManager.updateSkipLastInSec(podcast, newValue)
        }
    }

    fun unfollow() {
        viewModelScope.launch(NonCancellable) {
            podcastManager.unsubscribe(podcastUuid, playbackManager)
            tracker.track(
                AnalyticsEvent.PODCAST_UNSUBSCRIBED,
                mapOf(
                    "source" to SourceView.PODCAST_SETTINGS.analyticsValue,
                    "uuid" to podcastUuid,
                ),
            )
        }
    }

    fun addPodcastToPlaylists(playlistUuids: List<String>) {
        changePodcastRule(playlistUuids) { rule -> rule.withPodcast(podcastUuid) }
    }

    fun removePodcastFromPlaylists(playlistUuids: List<String>) {
        changePodcastRule(playlistUuids) { rule -> rule.withoutPodcast(podcastUuid) }
    }

    suspend fun getDownloadedEpisodeCount() = withContext(Dispatchers.IO) {
        podcastManager.countEpisodesInPodcastWithStatusBlocking(podcastUuid, EpisodeStatusEnum.DOWNLOADED)
    }

    private fun changePodcastRule(
        playlistUuids: List<String>,
        block: (PodcastsRule.Selected) -> PodcastsRule.Selected,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val playlistRules = uiState.value?.playlists
                ?.filter { it.uuid in playlistUuids }
                ?.associate { playlist ->
                    val podcastsRule = playlist.smartRules.podcasts
                    playlist.uuid to playlist.smartRules.copy(
                        podcasts = when (podcastsRule) {
                            is PodcastsRule.Any -> podcastsRule
                            is PodcastsRule.Selected -> block(podcastsRule)
                        },
                    )
                }
                .orEmpty()
            if (playlistRules.isNotEmpty()) {
                playlistManager.updateSmartRules(playlistRules)
            }
        }
    }

    private fun updatePlayerEffects(effects: PlaybackEffects) {
        val currentEpisode = playbackManager.upNextQueue.currentEpisode
        if (currentEpisode?.podcastOrSubstituteUuid == podcastUuid) {
            playbackManager.updatePlayerEffects(effects)
        }
    }

    data class UiState(
        val podcast: Podcast,
        val playlists: List<SmartPlaylistPreview>,
        val globalUpNextLimit: Int,
    ) {
        val selectedPlaylists = playlists
            .filter { playlist ->
                when (val rule = playlist.smartRules.podcasts) {
                    is PodcastsRule.Any -> true
                    is PodcastsRule.Selected -> podcast.uuid in rule.uuids
                }
            }
    }

    @AssistedFactory
    interface Factory {
        fun create(podcastUuid: String): PodcastSettingsViewModel
    }
}
