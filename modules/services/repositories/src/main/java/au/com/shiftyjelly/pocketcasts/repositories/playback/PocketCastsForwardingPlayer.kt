package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.net.Uri
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.media3.common.AdPlaybackState
import androidx.media3.common.C
import androidx.media3.common.FlagSet
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.HeartRating
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getArtworkUrl
import java.util.IdentityHashMap

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
    private val onSkipToQueueItem: ((String) -> Unit)? = null,
    internal val playGuard: (() -> Boolean) = { true },
    private val isAutomotive: Boolean = false,
) : ForwardingPlayer(wrappedPlayer) {

    private val isEmptyState: Boolean
        get() = isAutomotive && super.getCurrentTimeline().isEmpty

    internal var currentMediaItem: MediaItem = MediaItem.EMPTY
    internal var previousMediaId: String? = null
    internal var upNextItems: List<MediaItem> = emptyList()
        private set
    private var upNextCurrentItem: MediaItem? = null
    private var queueTimelineInputs: Triple<MediaItem, MediaItem?, List<MediaItem>>? = null
    private var queueTimeline: QueueTimeline? = null

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
        return PocketCastsForwardingPlayer(
            newPlayer,
            onSkipForward,
            onSkipBack,
            onStop,
            onPlay,
            onPause,
            onSeekTo,
            onSkipToQueueItem,
            playGuard,
            isAutomotive,
        ).also { it.copyStateFrom(this) }
    }

    @MainThread
    internal fun copyStateFrom(other: PocketCastsForwardingPlayer) {
        currentMediaItem = other.currentMediaItem
        previousMediaId = other.previousMediaId
        isTransientLoss = other.isTransientLoss
        upNextCurrentItem = other.upNextCurrentItem
        upNextItems = other.upNextItems
        queueTimelineInputs = other.queueTimelineInputs
        queueTimeline = other.queueTimeline
    }

    /**
     * Updates the metadata exposed via [getCurrentMediaItem]. Call this when the
     * current episode changes or when metadata is refreshed (e.g., artwork loaded).
     *
     * @param showArtwork When false, artwork URI and data are omitted from the metadata.
     * @param useEpisodeArtwork When true, prefer episode-specific artwork; when false, use podcast artwork.
     * @param artworkData Pre-compressed artwork bytes (e.g. WebP). Compression should
     *   happen off the main thread before calling this method.
     */
    @MainThread
    fun updateMetadata(
        episode: BaseEpisode,
        podcast: Podcast?,
        showArtwork: Boolean = true,
        useEpisodeArtwork: Boolean = true,
        artworkData: ByteArray? = null,
        artworkUri: Uri? = if (showArtwork) resolveArtworkUri(episode, podcast, useEpisodeArtwork) else null,
        showRating: Boolean = true,
    ) {
        checkMainThread()
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
            .setUserRating(if (showRating && episode is PodcastEpisode) buildRating(episode) else null)

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
        if (episodeChanged) {
            dispatchEvents(Player.EVENT_MEDIA_METADATA_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)
        } else {
            dispatchEvents(Player.EVENT_MEDIA_METADATA_CHANGED)
        }
        dispatchTimelineIfChanged()
    }

    /**
     * Clears metadata when nothing is playing, so the session doesn't show stale info.
     */
    @MainThread
    fun clearMetadata(title: String? = null) {
        checkMainThread()
        val metadata = MediaMetadata.Builder().setTitle(title).build()
        currentMediaItem = MediaItem.EMPTY.buildUpon().setMediaMetadata(metadata).build()
        previousMediaId = null
        isTransientLoss = false
        listeners.forEach { listener ->
            listener.onMediaMetadataChanged(metadata)
        }
        dispatchEvents(Player.EVENT_MEDIA_METADATA_CHANGED)
        dispatchTimelineIfChanged()
    }

    /**
     * Sets the Up Next episodes exposed after the current episode in [getCurrentTimeline], so
     * controllers such as Wear OS can show the queue.
     *
     * @param currentItem The Up Next head, used for the current episode's artwork when it matches.
     * @param upNext The episodes queued after the current one.
     */
    @MainThread
    fun updateQueue(currentItem: MediaItem?, upNext: List<MediaItem>) {
        checkMainThread()
        upNextCurrentItem = currentItem
        upNextItems = upNext
        dispatchTimelineIfChanged()
    }

    override fun getCurrentMediaItem(): MediaItem? = if (isEmptyState) null else currentMediaItem

    override fun getMediaMetadata(): MediaMetadata = currentMediaItem.mediaMetadata

    override fun isCommandAvailable(command: Int): Boolean {
        return if (isEmptyState || (supportsQueue && command in QUEUE_COMMANDS)) {
            availableCommands.contains(command)
        } else {
            super.isCommandAvailable(command)
        }
    }

    override fun getCurrentTimeline(): Timeline = queueTimeline ?: super.getCurrentTimeline()

    override fun getCurrentMediaItemIndex(): Int {
        return if (queueTimeline == null) super.getCurrentMediaItemIndex() else 0
    }

    override fun getCurrentPeriodIndex(): Int {
        return if (queueTimeline == null) super.getCurrentPeriodIndex() else 0
    }

    override fun getMediaItemCount(): Int = currentTimeline.windowCount

    override fun getMediaItemAt(index: Int): MediaItem {
        return currentTimeline.getWindow(index, Timeline.Window()).mediaItem
    }

    override fun getAvailableCommands(): Player.Commands {
        if (isEmptyState) {
            return Player.Commands.Builder()
                .addAll(
                    Player.COMMAND_SET_MEDIA_ITEM,
                    Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                    Player.COMMAND_GET_METADATA,
                )
                .build()
        }
        return Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_SET_MEDIA_ITEM,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_STOP,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_METADATA,
            )
            .addIf(Player.COMMAND_GET_TIMELINE, supportsQueue)
            .addIf(Player.COMMAND_SEEK_TO_MEDIA_ITEM, supportsQueue)
            .build()
    }

    override fun seekTo(positionMs: Long) {
        onSeekTo?.invoke(positionMs)
        super.seekTo(positionMs)
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        if (queueTimeline == null || mediaItemIndex == 0) {
            onSeekTo?.invoke(positionMs)
            super.seekTo(mediaItemIndex, positionMs)
        } else {
            skipToQueueItem(mediaItemIndex)
        }
    }

    override fun seekToDefaultPosition(mediaItemIndex: Int) {
        if (queueTimeline == null) {
            super.seekToDefaultPosition(mediaItemIndex)
        } else if (mediaItemIndex != 0) {
            skipToQueueItem(mediaItemIndex)
        }
    }

    private fun skipToQueueItem(mediaItemIndex: Int) {
        val timeline = queueTimeline ?: return
        if (mediaItemIndex !in 1 until timeline.windowCount) return
        val mediaId = timeline.getWindow(mediaItemIndex, Timeline.Window()).mediaItem.mediaId
        onSkipToQueueItem?.invoke(mediaId)
    }

    override fun seekToNext() {
        onSkipForward?.invoke() ?: super.seekToNext()
    }

    override fun seekToPrevious() {
        onSkipBack?.invoke() ?: super.seekToPrevious()
    }

    override fun stop() {
        // Either delegate to the app's stop handler (which pauses via PlaybackManager)
        // or fall through to the wrapped player. Calling both would stop the ExoPlayer
        // (releasing decoders) before PlaybackManager can interact with it.
        onStop?.invoke() ?: super.stop()
    }

    override fun play() {
        if (playGuard()) {
            // Either delegate to the app's play handler (which plays via PlaybackManager)
            // or fall through to the wrapped player. Calling both would cause double-play
            // because PlaybackManager.playQueueSuspend() already drives the player.
            onPlay?.invoke() ?: super.play()
        }
    }

    override fun pause() {
        // Same pattern as play() and stop() — the pause callback drives the player
        // through PlaybackManager, so calling both would cause double-pause.
        onPause?.invoke() ?: super.pause()
    }

    // ---- Media3 controller protocol interception ----
    // After onAddMediaItems resolves, Media3 calls setMediaItems → prepare → play.
    // We intercept setMediaItems/addMediaItems to update metadata from the resolved
    // MediaItem and intercept prepare() as a no-op, because actual playback preparation
    // is handled by PlaybackManager through the side-channel (playNowSuspend).

    override fun setMediaItems(mediaItems: MutableList<MediaItem>) {
        applyResolvedMediaItems(mediaItems)
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        applyResolvedMediaItems(mediaItems)
    }

    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        applyResolvedMediaItems(mediaItems)
    }

    override fun setMediaItem(mediaItem: MediaItem) {
        applyResolvedMediaItems(mutableListOf(mediaItem))
    }

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
        applyResolvedMediaItems(mutableListOf(mediaItem))
    }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
        applyResolvedMediaItems(mutableListOf(mediaItem))
    }

    override fun addMediaItems(mediaItems: MutableList<MediaItem>) {
        applyResolvedMediaItems(mediaItems)
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        applyResolvedMediaItems(mediaItems)
    }

    override fun addMediaItem(mediaItem: MediaItem) {
        applyResolvedMediaItems(mutableListOf(mediaItem))
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        applyResolvedMediaItems(mutableListOf(mediaItem))
    }

    override fun prepare() {
        // No-op — actual preparation is handled by PlaybackManager via playNowSuspend.
    }

    private fun applyResolvedMediaItems(mediaItems: List<MediaItem>) {
        val item = mediaItems.firstOrNull() ?: return
        val metadata = item.mediaMetadata
        val episodeChanged = previousMediaId != item.mediaId

        currentMediaItem = item
        previousMediaId = item.mediaId

        listeners.forEach { listener ->
            listener.onMediaMetadataChanged(metadata)
            if (episodeChanged) {
                listener.onMediaItemTransition(
                    currentMediaItem,
                    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
                )
            }
        }
        if (episodeChanged) {
            dispatchEvents(Player.EVENT_MEDIA_METADATA_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)
        } else {
            dispatchEvents(Player.EVENT_MEDIA_METADATA_CHANGED)
        }
        dispatchTimelineIfChanged()
    }

    override fun getDuration(): Long {
        val playerDuration = super.getDuration()
        if (playerDuration != C.TIME_UNSET) {
            return playerDuration
        }
        return currentMediaItem.mediaMetadata.durationMs ?: C.TIME_UNSET
    }

    private val listeners = java.util.concurrent.CopyOnWriteArrayList<Player.Listener>()

    private val queueTimelineListeners = IdentityHashMap<Player.Listener, QueueTimelineListener>()

    override fun addListener(listener: Player.Listener) {
        if (supportsQueue) {
            val queueTimelineListener = queueTimelineListeners.getOrPut(listener) {
                QueueTimelineListener(listener) { currentTimeline }
            }
            super.addListener(queueTimelineListener)
        } else {
            super.addListener(listener)
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    override fun removeListener(listener: Player.Listener) {
        super.removeListener(queueTimelineListeners.remove(listener) ?: listener)
        listeners.remove(listener)
    }

    private val supportsQueue: Boolean
        get() = !isAutomotive

    private fun buildQueueTimeline(): QueueTimeline? {
        if (!supportsQueue || currentMediaItem.mediaId == MediaItem.DEFAULT_MEDIA_ID) return null
        val metadata = currentMediaItem.mediaMetadata
        val upNextArtworkUri = upNextCurrentItem
            ?.takeIf { it.mediaId == currentMediaItem.mediaId }
            ?.mediaMetadata
            ?.artworkUri
        val current = currentMediaItem.buildUpon()
            .setMediaMetadata(
                metadata.buildUpon()
                    .setArtworkData(null, null)
                    .setArtworkUri(metadata.artworkUri?.let { upNextArtworkUri ?: it })
                    .build(),
            )
            .build()
        val upNext = upNextItems.filter { it.mediaId != current.mediaId }.distinctBy { it.mediaId }
        return QueueTimeline(listOf(current) + upNext)
    }

    private fun dispatchTimelineIfChanged() {
        val inputs = Triple(currentMediaItem, upNextCurrentItem, upNextItems)
        if (inputs == queueTimelineInputs) return
        queueTimelineInputs = inputs
        val newTimeline = buildQueueTimeline()
        if (newTimeline == queueTimeline) return
        queueTimeline = newTimeline
        val timeline = currentTimeline
        listeners.forEach { it.onTimelineChanged(timeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) }
        dispatchEvents(Player.EVENT_TIMELINE_CHANGED)
    }

    private fun resolveArtworkUri(episode: BaseEpisode, podcast: Podcast?, useEpisodeArtwork: Boolean): Uri? {
        return when (episode) {
            is PodcastEpisode -> {
                val url = if (useEpisodeArtwork) {
                    episode.imageUrl?.takeIf { it.isNotBlank() }
                        ?: podcast?.getArtworkUrl(480)?.takeIf { it.isNotBlank() }
                } else {
                    podcast?.getArtworkUrl(480)?.takeIf { it.isNotBlank() }
                }
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

    /**
     * Dispatches a batched [Player.Events] callback to all listeners.
     * Media3's notification system reacts to [Player.Listener.onEvents], not
     * individual callbacks, so this must be called after individual callbacks.
     */
    private fun dispatchEvents(vararg eventFlags: @Player.Event Int) {
        val events = Player.Events(FlagSet.Builder().addAll(*eventFlags).build())
        listeners.forEach { it.onEvents(this@PocketCastsForwardingPlayer, events) }
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "PocketCastsForwardingPlayer must be accessed on the main thread"
        }
    }

    private companion object {
        val QUEUE_COMMANDS = setOf(Player.COMMAND_GET_TIMELINE, Player.COMMAND_SEEK_TO_MEDIA_ITEM)
    }
}

@OptIn(UnstableApi::class)
private class QueueTimeline(val items: List<MediaItem>) : Timeline() {
    private val indexByMediaId = items.withIndex().associate { (index, item) -> item.mediaId to index }

    override fun getWindowCount() = items.size

    override fun getWindow(windowIndex: Int, window: Window, defaultPositionProjectionUs: Long): Window {
        val item = items[windowIndex]
        return window.set(
            item.mediaId,
            item,
            null,
            C.TIME_UNSET,
            C.TIME_UNSET,
            C.TIME_UNSET,
            true,
            false,
            null,
            0,
            item.durationUs(),
            windowIndex,
            windowIndex,
            0,
        )
    }

    override fun getPeriodCount() = items.size

    override fun getPeriod(periodIndex: Int, period: Period, setIds: Boolean): Period {
        val mediaId = items[periodIndex].mediaId
        val durationUs = items[periodIndex].durationUs()
        return period.set(mediaId, mediaId, periodIndex, durationUs, 0, AdPlaybackState.NONE, false)
    }

    override fun getIndexOfPeriod(uid: Any) = indexByMediaId[uid] ?: C.INDEX_UNSET

    override fun getUidOfPeriod(periodIndex: Int): Any = items[periodIndex].mediaId

    private fun MediaItem.durationUs(): Long {
        return mediaMetadata.durationMs?.takeIf { it > 0 }?.let(Util::msToUs) ?: C.TIME_UNSET
    }

    override fun equals(other: Any?) = other is QueueTimeline && other.items == items

    override fun hashCode() = items.hashCode()
}
