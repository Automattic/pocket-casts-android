package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * A [SimpleBasePlayer] that mirrors the playback state of a [CastPlayer] (Pocket Casts'
 * own cast implementation) into Media3's player model.
 *
 * This player is installed into the Media3 session during cast playback so that
 * notifications, lock screen controls, and `onTaskRemoved()` see the correct
 * play/pause/buffering state. Metadata is handled separately by
 * [PocketCastsForwardingPlayer.updateMetadata].
 *
 * Transport commands (play, pause, seek, stop) are delegated to [PlaybackManager]
 * via the provided callbacks.
 */
@OptIn(UnstableApi::class)
internal class CastStatePlayer(
    applicationLooper: Looper,
    private val onPlay: () -> Unit,
    private val onPause: () -> Unit,
    private val onSeekTo: (Long) -> Unit,
    private val onStop: () -> Unit,
) : SimpleBasePlayer(applicationLooper) {

    private var castPlaying = false
    private var castBuffering = false
    private var castPositionMs = 0L
    private var hasReceivedUpdate = false

    /**
     * A single placeholder item so that [SimpleBasePlayer] accepts non-idle states.
     * The real metadata is managed by [PocketCastsForwardingPlayer.updateMetadata].
     */
    private val placeholderItem = MediaItemData.Builder("cast-placeholder")
        .setMediaItem(MediaItem.EMPTY)
        .build()

    /**
     * Updates the cast playback state. Call this from the main thread whenever
     * [CastPlayer] reports a state change (playing, paused, buffering).
     *
     * Calls [invalidateState] so Media3 re-reads [getState] and notifies listeners.
     */
    fun updateCastState(isPlaying: Boolean, isBuffering: Boolean, positionMs: Long) {
        castPlaying = isPlaying
        castBuffering = isBuffering
        castPositionMs = positionMs
        hasReceivedUpdate = true
        invalidateState()
    }

    override fun getState(): State {
        if (!hasReceivedUpdate) {
            return State.Builder()
                .setAvailableCommands(Player.Commands.EMPTY)
                .setPlaybackState(STATE_IDLE)
                .build()
        }
        return State.Builder()
            .setAvailableCommands(
                Player.Commands.Builder()
                    .addAll(
                        COMMAND_PLAY_PAUSE,
                        COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                        COMMAND_STOP,
                        COMMAND_GET_CURRENT_MEDIA_ITEM,
                        COMMAND_GET_METADATA,
                    )
                    .build(),
            )
            .setPlaylist(listOf(placeholderItem))
            .setPlayWhenReady(castPlaying || castBuffering, PLAY_WHEN_READY_CHANGE_REASON_REMOTE)
            .setPlaybackState(
                when {
                    castBuffering -> STATE_BUFFERING
                    else -> STATE_READY
                },
            )
            .setContentPositionMs(castPositionMs)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) {
            onPlay()
        } else {
            onPause()
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: @Player.Command Int,
    ): ListenableFuture<*> {
        onSeekTo(positionMs)
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        onStop()
        return Futures.immediateVoidFuture()
    }
}
