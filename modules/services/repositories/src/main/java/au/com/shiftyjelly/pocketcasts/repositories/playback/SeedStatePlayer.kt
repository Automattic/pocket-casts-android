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
 * A [SimpleBasePlayer] that provides a "paused with last episode" player state
 * before real playback begins.
 *
 * Before [seed] is called the player reports [STATE_IDLE] with empty commands,
 * identical to the previous ExoPlayer placeholder behaviour. After [seed] it
 * transitions to [STATE_READY] with `playWhenReady = false`, a single
 * placeholder media item, and the saved playback position. This gives AAOS
 * (and other Media3 controllers) a non-empty timeline so that the Now Playing
 * screen is displayed immediately on cold start.
 *
 * Transport command handlers are no-ops because the wrapping
 * [PocketCastsForwardingPlayer] intercepts all commands before they reach this
 * player.
 */
@OptIn(UnstableApi::class)
internal class SeedStatePlayer(
    applicationLooper: Looper,
) : SimpleBasePlayer(applicationLooper) {

    private var seeded = false
    private var positionMs = 0L

    private val placeholderItem = MediaItemData.Builder("seed-placeholder")
        .setMediaItem(MediaItem.EMPTY)
        .build()

    /**
     * Transitions the player to [STATE_READY] with a single placeholder item
     * at the given position. Call on the main thread.
     */
    fun seed(positionMs: Long) {
        seeded = true
        this.positionMs = positionMs
        invalidateState()
    }

    override fun getState(): State {
        if (!seeded) {
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
            .setPlayWhenReady(false, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(STATE_READY)
            .setContentPositionMs(positionMs)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: @Player.Command Int,
    ): ListenableFuture<*> {
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        return Futures.immediateVoidFuture()
    }
}
