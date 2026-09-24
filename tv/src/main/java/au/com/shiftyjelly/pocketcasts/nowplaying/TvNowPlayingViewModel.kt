package au.com.shiftyjelly.pocketcasts.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffectsData
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.Player
import au.com.shiftyjelly.pocketcasts.repositories.playback.StreamVideoState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PlaybackContentType
import com.automattic.eventhorizon.PlaybackEffectSpeedChangedEvent
import com.automattic.eventhorizon.PlaybackEffectTrimSilenceAmountChangedEvent
import com.automattic.eventhorizon.PlaybackEffectVolumeBoostToggledEvent
import com.automattic.eventhorizon.PlayerDismissedEvent
import com.automattic.eventhorizon.PlayerShownEvent
import com.automattic.eventhorizon.SettingType
import com.automattic.eventhorizon.SourceViewType
import com.automattic.eventhorizon.Trackable
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class TvNowPlayingViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
    private val eventHorizon: EventHorizon,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    val uiState: StateFlow<TvNowPlayingUiState> = combine(
        playbackManager.playbackStateFlow,
        playbackManager.upNextQueue.changesObservable.asFlow(),
        playbackManager.playerFlow,
        playbackManager.streamVideoState,
        playbackManager.videoRenderingEnabled,
    ) { playbackState, queueState, player, streamVideoState, videoRenderingEnabled ->
        if (queueState is UpNextQueue.State.Loaded) {
            val episode = queueState.episode
            // The queue and playback relays update at different times during an episode switch, so
            // fall back to the entity's progress until the playback state refers to this episode.
            val isPlaybackStateCurrent = playbackState.episodeUuid == episode.uuid
            TvNowPlayingUiState.Loaded(
                episode = episode,
                podcastTitle = queueState.podcast?.title,
                isPlaying = playbackState.isPlaying,
                isBuffering = playbackState.isBuffering,
                errorMessage = playbackState.lastErrorMessage.takeIf { playbackState.isError },
                positionMs = if (isPlaybackStateCurrent) playbackState.positionMs else episode.playedUpToMs,
                durationMs = if (isPlaybackStateCurrent) playbackState.durationMs else episode.durationMs,
                bufferedMs = if (isPlaybackStateCurrent) playbackState.bufferedMs else 0,
                isVideo = isVideo(episode, streamVideoState, videoRenderingEnabled),
                player = player,
                playbackSpeed = playbackState.playbackSpeed,
                trimMode = playbackState.trimMode,
                isVolumeBoosted = playbackState.isVolumeBoosted,
            )
        } else {
            TvNowPlayingUiState.Empty
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TvNowPlayingUiState.Empty,
    )

    fun trackPlayerShown() {
        eventHorizon.track(PlayerShownEvent)
    }

    fun trackPlayerDismissed() {
        eventHorizon.track(PlayerDismissedEvent)
    }

    fun playPause() {
        playbackManager.playPause(sourceView = SourceView.PLAYER)
    }

    fun skipForward() {
        playbackManager.skipForward(
            sourceView = SourceView.PLAYER,
            jumpAmountSeconds = settings.skipForwardInSecs.value,
        )
    }

    fun skipBackward() {
        playbackManager.skipBackward(
            sourceView = SourceView.PLAYER,
            jumpAmountSeconds = settings.skipBackInSecs.value,
        )
    }

    fun setPlaybackSpeed(speed: Double) = updateEffects(
        update = { it.copy(playbackSpeed = speed) },
        event = { setting, source, contentType ->
            PlaybackEffectSpeedChangedEvent(speed = speed, settings = setting, source = source, contentType = contentType)
        },
    )

    fun setTrimMode(trimMode: TrimMode) = updateEffects(
        update = { it.copy(trimMode = trimMode) },
        event = { setting, source, contentType ->
            PlaybackEffectTrimSilenceAmountChangedEvent(
                amount = trimMode.analyticsValue,
                settings = setting,
                source = source,
                contentType = contentType,
            )
        },
    )

    fun setVolumeBoost(isBoosted: Boolean) = updateEffects(
        update = { it.copy(isVolumeBoosted = isBoosted) },
        event = { setting, source, contentType ->
            PlaybackEffectVolumeBoostToggledEvent(enabled = isBoosted, settings = setting, source = source, contentType = contentType)
        },
    )

    private val effectsMutex = Mutex()
    private var pendingEffects: PlaybackEffectsData? = null
    private var pendingEffectsBaseline: PlaybackEffectsData? = null
    private var pendingEffectsEpisodeUuid: String? = null

    private fun updateEffects(
        update: (PlaybackEffectsData) -> PlaybackEffectsData,
        event: (SettingType, SourceViewType, PlaybackContentType) -> Trackable,
    ) {
        viewModelScope.launch(ioDispatcher) {
            effectsMutex.withLock {
                val playbackState = playbackManager.playbackStateFlow.first()
                val currentEpisode = playbackManager.getCurrentEpisode()
                // Skip the write while the playback state still points at the previous episode, so a
                // stale snapshot cannot be persisted onto the episode that is now current.
                if (currentEpisode == null || playbackState.episodeUuid != currentEpisode.uuid) {
                    return@withLock
                }
                val stateEffects = PlaybackEffectsData(
                    playbackSpeed = playbackState.playbackSpeed,
                    trimMode = playbackState.trimMode,
                    isVolumeBoosted = playbackState.isVolumeBoosted,
                )
                val baseEffects = pendingEffects
                    ?.takeIf { pendingEffectsEpisodeUuid == currentEpisode.uuid && stateEffects == pendingEffectsBaseline }
                    ?: stateEffects
                val updatedEffects = update(baseEffects)
                pendingEffects = updatedEffects
                pendingEffectsBaseline = stateEffects
                pendingEffectsEpisodeUuid = currentEpisode.uuid
                val effects = updatedEffects.toEffects()
                val overridingPodcast = (currentEpisode as? PodcastEpisode)
                    ?.let { podcastManager.findPodcastByUuid(it.podcastUuid) }
                    ?.takeIf { it.overrideGlobalEffects }
                if (overridingPodcast != null) {
                    podcastManager.updateEffectsBlocking(overridingPodcast, effects)
                } else {
                    settings.globalPlaybackEffects.set(effects, updateModifiedAt = true)
                }
                playbackManager.updatePlayerEffects(effects)
                val setting = if (overridingPodcast != null) SettingType.Local else SettingType.Global
                playbackManager.trackPlaybackEvent(SourceView.PLAYER_PLAYBACK_EFFECTS) { source, contentType ->
                    event(setting, source.analyticsValue, contentType)
                }
            }
        }
    }

    private fun isVideo(
        episode: BaseEpisode,
        streamVideoState: StreamVideoState,
        videoRenderingEnabled: Boolean,
    ) = videoRenderingEnabled && when (streamVideoState) {
        StreamVideoState.HasVideo -> true
        StreamVideoState.Unknown, StreamVideoState.AudioOnly -> false
        StreamVideoState.NotVideo -> episode.isVideo
    }
}

sealed interface TvNowPlayingUiState {
    data object Empty : TvNowPlayingUiState

    data class Loaded(
        val episode: BaseEpisode,
        val podcastTitle: String?,
        val isPlaying: Boolean,
        val isBuffering: Boolean,
        val errorMessage: String?,
        val positionMs: Int,
        val durationMs: Int,
        val bufferedMs: Int,
        val isVideo: Boolean,
        val player: Player?,
        val playbackSpeed: Double,
        val trimMode: TrimMode,
        val isVolumeBoosted: Boolean,
    ) : TvNowPlayingUiState
}
