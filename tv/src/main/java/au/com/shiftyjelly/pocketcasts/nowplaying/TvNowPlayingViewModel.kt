package au.com.shiftyjelly.pocketcasts.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Live playback state for the TV player, plus transport commands.
 *
 * Skip amounts are deliberately NOT hardcoded: PlaybackManager.skipForward()
 * and skipBackward() default to the user's own skipForwardInSecs /
 * skipBackInSecs settings, so this matches the phone app automatically.
 */
@HiltViewModel
class TvNowPlayingViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState?> = playbackManager.playbackStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val skipBackSeconds: Int get() = settings.skipBackInSecs.value
    val skipForwardSeconds: Int get() = settings.skipForwardInSecs.value

    /**
     * The live player instance. Needed so the video SurfaceView can attach
     * itself via SimplePlayer.setDisplay() - the same hook the phone app's
     * VideoView uses.
     */
    val player: StateFlow<Player?> = playbackManager.playerFlow

    /** True when the episode being played carries video (e.g. TWiT video feeds). */
    val isVideoEpisode: Boolean get() = playbackManager.getCurrentEpisode()?.isVideo == true

    fun playPause() = playbackManager.playPause()

    fun skipBackward() = playbackManager.skipBackward()

    fun skipForward() = playbackManager.skipForward()

    /**
     * Cycles 1x -> 1.2x -> 1.5x -> 2x -> 1x, saving to the global effects
     * setting and pushing to the running player - the same two steps the phone
     * player performs in saveEffects().
     */
    fun cycleSpeed() {
        val effects = settings.globalPlaybackEffects.value
        effects.playbackSpeed = when {
            effects.playbackSpeed < 1.15 -> 1.2
            effects.playbackSpeed < 1.45 -> 1.5
            effects.playbackSpeed < 1.95 -> 2.0
            else -> 1.0
        }
        settings.globalPlaybackEffects.set(effects, updateModifiedAt = true)
        playbackManager.updatePlayerEffects(effects)
    }
}
