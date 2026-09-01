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
     * True when the host app (Pocket Casts) is actively playing audio. Used by
     * [OtherAppPlayingCondition], together with a short time-bounded recency window,
     * to distinguish the host's own audio from another app's during a play/pause/route
     * transition (see voice-control-core).
     */
    val isHostAudioActive: StateFlow<Boolean> = context.map {
        it is PlaybackContext.Active && it.isPlaying
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
