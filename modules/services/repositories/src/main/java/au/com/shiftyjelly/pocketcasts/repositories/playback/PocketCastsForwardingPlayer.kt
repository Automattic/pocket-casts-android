package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.net.Uri
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.HeartRating
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getArtworkUrl

/**
 * Bridges the app's playback layer to Media3's [Player] interface.
 *
 * Wraps an ExoPlayer (from [SimplePlayer.exoPlayer]) and enriches it with
 * episode metadata so that a Media3 MediaSession can read the current media item
 * directly from the player.
 *
 * This player must only be accessed on the main thread, consistent with
 * ExoPlayer's threading requirements.
 *
 * **Player swapping** (local ↔ cast): Call [swapPlayer] to create a new
 * [PocketCastsForwardingPlayer] wrapping a different player while preserving
 * metadata state. The caller then installs the new player on the MediaSession
 * via `MediaSession.setPlayer()`.
 */
@OptIn(UnstableApi::class)
class PocketCastsForwardingPlayer(
    wrappedPlayer: Player,
    private val onSkipForward: (() -> Unit)? = null,
    private val onSkipBack: (() -> Unit)? = null,
    private val onStop: (() -> Unit)? = null,
    private val onPlay: (() -> Unit)? = null,
    private val onPause: (() -> Unit)? = null,
    private val onSeekTo: ((Long) -> Unit)? = null,
    private val playGuard: (() -> Boolean) = { true },
) : ForwardingPlayer(wrappedPlayer) {

    private var currentMediaItem: MediaItem = MediaItem.EMPTY
    private var previousMediaId: String? = null

    /**
     * Indicates the player is in a transient loss state (e.g., audio focus lost temporarily).
     * Media3 has no built-in equivalent. The notification provider reads this to decide
     * whether to keep the foreground service alive.
     */
    var isTransientLoss: Boolean = false

    /**
     * Creates a new [PocketCastsForwardingPlayer] wrapping [newPlayer] while
     * preserving the current metadata and transient loss state.
     *
     * After calling this, install the returned player on the MediaSession:
     * ```
     * val newForwardingPlayer = forwardingPlayer.swapPlayer(castPlayer)
     * mediaSession.setPlayer(newForwardingPlayer)
     * ```
     */
    @MainThread
    fun swapPlayer(newPlayer: Player): PocketCastsForwardingPlayer {
        checkMainThread()
        return PocketCastsForwardingPlayer(newPlayer, onSkipForward, onSkipBack, onStop, onPlay, onPause, onSeekTo, playGuard).also {
            it.currentMediaItem = this.currentMediaItem
            it.previousMediaId = this.previousMediaId
            it.isTransientLoss = this.isTransientLoss
        }
    }

    /**
     * Updates the metadata exposed via [getCurrentMediaItem]. Call this when the
     * current episode changes or when metadata is refreshed (e.g., artwork loaded).
     */
    @MainThread
    fun updateMetadata(
        episode: BaseEpisode,
        podcast: Podcast?,
        showArtwork: Boolean = true,
        artworkData: ByteArray? = null,
    ) {
        checkMainThread()

        val artworkUri = if (showArtwork) resolveArtworkUri(episode, podcast) else null
        val podcastTitle = episode.displaySubtitle(podcast)

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtist(podcastTitle)
            .setAlbumTitle(podcast?.author?.takeIf { it.isNotEmpty() })
            .setGenre("Podcast")
            .setArtworkUri(artworkUri)
            .setDurationMs(episode.durationMs.toLong())
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
            .setUserRating(if (episode is PodcastEpisode) buildRating(episode) else null)

        if (showArtwork && artworkData != null) {
            metadataBuilder.setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }

        val metadata = metadataBuilder.build()

        val episodeChanged = previousMediaId != episode.uuid
        previousMediaId = episode.uuid

        currentMediaItem = MediaItem.Builder()
            .setMediaId(episode.uuid)
            .setMediaMetadata(metadata)
            .build()

        listeners.forEach { listener ->
            listener.onMediaMetadataChanged(metadata)
            if (episodeChanged) {
                listener.onMediaItemTransition(
                    currentMediaItem,
                    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
                )
            }
        }
    }

    override fun getCurrentMediaItem(): MediaItem = currentMediaItem

    override fun getMediaMetadata(): MediaMetadata = currentMediaItem.mediaMetadata

    override fun getAvailableCommands(): Player.Commands {
        return Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_FORWARD,
                Player.COMMAND_SEEK_BACK,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_STOP,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_SET_MEDIA_ITEM,
            )
            .build()
    }

    override fun seekTo(positionMs: Long) {
        onSeekTo?.invoke(positionMs)
        super.seekTo(positionMs)
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        onSeekTo?.invoke(positionMs)
        super.seekTo(mediaItemIndex, positionMs)
    }

    override fun seekToNext() {
        onSkipForward?.invoke() ?: super.seekToNext()
    }

    override fun seekToPrevious() {
        onSkipBack?.invoke() ?: super.seekToPrevious()
    }

    override fun stop() {
        onStop?.invoke() ?: super.stop()
    }

    override fun play() {
        if (playGuard()) {
            super.play()
            onPlay?.invoke()
        }
    }

    override fun pause() {
        super.pause()
        onPause?.invoke()
    }

    override fun getDuration(): Long {
        val playerDuration = super.getDuration()
        if (playerDuration != C.TIME_UNSET) {
            return playerDuration
        }
        return currentMediaItem.mediaMetadata.durationMs ?: C.TIME_UNSET
    }

    private val listeners = java.util.concurrent.CopyOnWriteArrayList<Player.Listener>()

    override fun addListener(listener: Player.Listener) {
        super.addListener(listener)
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    override fun removeListener(listener: Player.Listener) {
        super.removeListener(listener)
        listeners.remove(listener)
    }

    private fun resolveArtworkUri(episode: BaseEpisode, podcast: Podcast?): Uri? {
        return when (episode) {
            is PodcastEpisode -> {
                val url = episode.imageUrl?.takeIf { it.isNotBlank() }
                    ?: podcast?.getArtworkUrl(480)?.takeIf { it.isNotBlank() }
                url?.let(Uri::parse)
            }

            is UserEpisode -> {
                episode.artworkUrl?.takeIf { it.isNotBlank() }?.let(Uri::parse)
            }
        }
    }

    private fun buildRating(episode: BaseEpisode): HeartRating {
        return HeartRating(episode.isStarred)
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "PocketCastsForwardingPlayer must be accessed on the main thread"
        }
    }
}
