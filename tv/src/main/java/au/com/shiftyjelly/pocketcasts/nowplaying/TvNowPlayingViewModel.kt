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

@HiltViewModel
class TvNowPlayingViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
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

    fun setPlaybackSpeed(speed: Double) = updateEffects { it.copy(playbackSpeed = speed) }

    fun setTrimMode(trimMode: TrimMode) = updateEffects { it.copy(trimMode = trimMode) }

    fun toggleVolumeBoost() = updateEffects { it.copy(isVolumeBoosted = !it.isVolumeBoosted) }

    private fun updateEffects(update: (PlaybackEffectsData) -> PlaybackEffectsData) {
        viewModelScope.launch(ioDispatcher) {
            val playbackState = playbackManager.playbackStateFlow.first()
            val currentEffects = PlaybackEffectsData(
                playbackSpeed = playbackState.playbackSpeed,
                trimMode = playbackState.trimMode,
                isVolumeBoosted = playbackState.isVolumeBoosted,
            )
            val effects = update(currentEffects).toEffects()
            val podcast = (playbackManager.getCurrentEpisode() as? PodcastEpisode)
                ?.let { podcastManager.findPodcastByUuid(it.podcastUuid) }
            if (podcast != null && podcast.overrideGlobalEffects) {
                podcastManager.updateEffectsBlocking(podcast, effects)
            } else {
                settings.globalPlaybackEffects.set(effects, updateModifiedAt = true)
            }
            playbackManager.updatePlayerEffects(effects)
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
