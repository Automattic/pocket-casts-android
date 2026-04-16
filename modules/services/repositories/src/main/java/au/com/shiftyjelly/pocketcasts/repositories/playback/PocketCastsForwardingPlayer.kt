package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.net.Uri
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.FlagSet
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.HeartRating
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getArtworkUrl

/**
 * Wraps an ExoPlayer and enriches it with episode metadata and an up next
 * queue timeline so that Media3 MediaSession exposes them to external controllers.
 *
 * Must only be accessed on the main thread.
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
    private val onSeekToQueueItem: ((mediaId: String) -> Unit)? = null,
    internal val playGuard: (() -> Boolean) = { true },
) : ForwardingPlayer(wrappedPlayer) {

    internal var currentMediaItem: MediaItem = MediaItem.EMPTY
    internal var previousMediaId: String? = null
    internal var queueItems: List<MediaItem> = emptyList()

    var isTransientLoss: Boolean = false

    @MainThread
    fun swapPlayer(newPlayer: Player): PocketCastsForwardingPlayer {
        checkMainThread()
        return PocketCastsForwardingPlayer(newPlayer, onSkipForward, onSkipBack, onStop, onPlay, onPause, onSeekTo, onSeekToQueueItem, playGuard).also {
            it.currentMediaItem = this.currentMediaItem
            it.previousMediaId = this.previousMediaId
            it.isTransientLoss = this.isTransientLoss
            it.queueItems = this.queueItems
        }
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
                // The first window of the timeline tracks currentMediaItem, so changing the
                // episode also changes the timeline. The legacy MediaSessionCompat bridge
                // relies on this event to rebuild setQueue for external controllers (Wear OS).
                listener.onTimelineChanged(currentTimeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
            }
        }
        if (episodeChanged) {
            dispatchEvents(
                Player.EVENT_MEDIA_METADATA_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_TIMELINE_CHANGED,
            )
        } else {
            dispatchEvents(Player.EVENT_MEDIA_METADATA_CHANGED)
        }
    }

    /** @param items Up next episodes, not including the currently playing episode. */
    @MainThread
    fun updateQueue(items: List<MediaItem>) {
        checkMainThread()
        queueItems = items
        listeners.forEach { listener ->
            listener.onTimelineChanged(currentTimeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        }
        dispatchEvents(Player.EVENT_TIMELINE_CHANGED)
    }

    @MainThread
    fun clearMetadata() {
        checkMainThread()
        currentMediaItem = MediaItem.EMPTY
        previousMediaId = null
        isTransientLoss = false
        queueItems = emptyList()
        listeners.forEach { listener ->
            listener.onMediaMetadataChanged(MediaMetadata.EMPTY)
            listener.onTimelineChanged(currentTimeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        }
        dispatchEvents(Player.EVENT_MEDIA_METADATA_CHANGED, Player.EVENT_TIMELINE_CHANGED)
    }

    override fun getCurrentMediaItem(): MediaItem = currentMediaItem

    override fun getMediaMetadata(): MediaMetadata = currentMediaItem.mediaMetadata

    override fun getCurrentTimeline(): Timeline {
        val allItems = buildList {
            if (currentMediaItem != MediaItem.EMPTY) add(currentMediaItem)
            addAll(queueItems)
        }
        return if (allItems.isEmpty()) Timeline.EMPTY else QueueTimeline(allItems)
    }

    override fun getCurrentMediaItemIndex(): Int {
        // Per Player contract, return C.INDEX_UNSET when there's no current window.
        // Returning 0 would make controllers think the first queue item is currently
        // playing when playback is idle, and prevents the legacy bridge from setting
        // a valid active queue item id (breaks Wear OS Up Next affordance).
        return if (currentMediaItem == MediaItem.EMPTY) C.INDEX_UNSET else 0
    }

    override fun getMediaItemCount(): Int = currentTimeline.windowCount

    override fun getMediaItemAt(index: Int): MediaItem {
        val timeline = currentTimeline
        val window = Timeline.Window()
        timeline.getWindow(index, window)
        return window.mediaItem
    }

    override fun getAvailableCommands(): Player.Commands {
        return Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_SET_MEDIA_ITEM,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_STOP,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_GET_TIMELINE,
            )
            .build()
    }

    override fun seekTo(positionMs: Long) {
        onSeekTo?.invoke(positionMs)
        super.seekTo(positionMs)
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        val queueCallback = onSeekToQueueItem
        if (queueCallback != null && mediaItemIndex > 0 && mediaItemIndex < 1 + queueItems.size) {
            val mediaId = queueItems[mediaItemIndex - 1].mediaId
            queueCallback.invoke(mediaId)
        } else {
            // No queue callback wired (e.g. cast install path) or index outside the
            // queue range — delegate to the wrapped player, matching seekTo(positionMs).
            onSeekTo?.invoke(positionMs)
            super.seekTo(mediaItemIndex, positionMs)
        }
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
            onPlay?.invoke() ?: super.play()
        }
    }

    override fun pause() {
        onPause?.invoke() ?: super.pause()
    }

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

    override fun prepare() = Unit

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

    private fun resolveArtworkUri(episode: BaseEpisode, podcast: Podcast?, useEpisodeArtwork: Boolean): Uri? {
        return when (episode) {
            is PodcastEpisode -> {
                val url = if (useEpisodeArtwork) {
                    episode.imageUrl?.takeIf { it.isNotBlank() }
                        ?: podcast?.getArtworkUrl(480)?.takeIf { it.isNotBlank() }
                } else {
                    podcast?.getArtworkUrl(480)?.takeIf { it.isNotBlank() }
                        ?: episode.imageUrl?.takeIf { it.isNotBlank() }
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

    private class QueueTimeline(private val items: List<MediaItem>) : Timeline() {
        override fun getWindowCount() = items.size

        override fun getWindow(windowIndex: Int, window: Window, defaultPositionProjectionUs: Long): Window {
            val item = items[windowIndex]
            val durationUs = item.mediaMetadata.durationMs?.let { it * 1000 } ?: C.TIME_UNSET
            window.set(
                /* uid = */
                windowIndex,
                /* mediaItem = */
                item,
                /* manifest = */
                null,
                /* presentationStartTimeMs = */
                C.TIME_UNSET,
                /* windowStartTimeMs = */
                C.TIME_UNSET,
                /* elapsedRealtimeEpochOffsetMs = */
                C.TIME_UNSET,
                /* isSeekable = */
                true,
                /* isDynamic = */
                false,
                /* liveConfiguration = */
                null,
                /* defaultPositionUs = */
                0L,
                /* durationUs = */
                durationUs,
                /* firstPeriodIndex = */
                windowIndex,
                /* lastPeriodIndex = */
                windowIndex,
                /* positionInFirstPeriodUs = */
                0L,
            )
            return window
        }

        override fun getPeriodCount() = items.size

        override fun getPeriod(periodIndex: Int, period: Period, setIds: Boolean): Period {
            period.set(
                /* id = */
                periodIndex,
                /* uid = */
                periodIndex,
                /* windowIndex = */
                periodIndex,
                /* durationUs = */
                C.TIME_UNSET,
                /* positionInWindowUs = */
                0L,
            )
            return period
        }

        override fun getIndexOfPeriod(uid: Any): Int {
            return if (uid is Int && uid in items.indices) uid else C.INDEX_UNSET
        }

        override fun getUidOfPeriod(periodIndex: Int) = periodIndex
    }
}
