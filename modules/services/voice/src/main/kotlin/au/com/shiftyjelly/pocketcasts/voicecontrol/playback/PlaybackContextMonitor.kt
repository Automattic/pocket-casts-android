package au.com.shiftyjelly.pocketcasts.voicecontrol.playback

import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface PlaybackContext {
    data class Active(val currentEpisodeUuid: String, val isPlaying: Boolean) : PlaybackContext
    data object Inactive : PlaybackContext
}

@Singleton
class PlaybackContextMonitor @javax.inject.Inject constructor(
    playbackManager: PlaybackManager,
    @ApplicationScope
    scope: CoroutineScope,
) {
    val context: StateFlow<PlaybackContext> = playbackManager.playbackStateFlow
        .map(::toPlaybackContext)
        .stateIn(scope, SharingStarted.Eagerly, PlaybackContext.Inactive)

    /**
     * True when the host app holds an active playback context (a loaded episode,
     * playing or paused). Used by [OtherAppPlayingCondition] to attribute an active
     * audio session to the host rather than to another app, so the mic is not blocked
     * over the host's own audio during a play/pause/route transition (see
     * voice-control-core).
     */
    val hasActiveContext: StateFlow<Boolean> = context.map {
        it is PlaybackContext.Active
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private fun toPlaybackContext(playbackState: PlaybackState): PlaybackContext {
        val currentEpisodeUuid = playbackState.episodeUuid
        return if (currentEpisodeUuid.isNotBlank() && !playbackState.isStopped && !playbackState.isEmpty) {
            PlaybackContext.Active(currentEpisodeUuid = currentEpisodeUuid, isPlaying = playbackState.isPlaying)
        } else {
            PlaybackContext.Inactive
        }
    }
}
