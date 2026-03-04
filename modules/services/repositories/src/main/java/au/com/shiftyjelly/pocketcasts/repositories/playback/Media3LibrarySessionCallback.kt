package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * [MediaLibraryService.MediaLibrarySession.Callback] that provides browse tree and search
 * functionality using [BrowseTreeProvider] and delegates session-level operations
 * (onConnect, onCustomCommand, onAddMediaItems, onSetRating, onMediaButtonEvent) to
 * [Media3SessionCallback].
 *
 * This callback is created but not wired in this PR — it will be used when the service
 * base class changes from [MediaBrowserServiceCompat] to [MediaLibraryService].
 */
@OptIn(UnstableApi::class)
internal class Media3LibrarySessionCallback(
    private val sessionCallback: Media3SessionCallback,
    private val browseTreeProvider: BrowseTreeProvider,
    private val scope: CoroutineScope,
    private val contextProvider: () -> android.content.Context,
) : MediaLibraryService.MediaLibrarySession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        return sessionCallback.onConnect(session, controller)
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        return sessionCallback.onCustomCommand(session, controller, customCommand, args)
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
    ): ListenableFuture<List<MediaItem>> {
        return sessionCallback.onAddMediaItems(mediaSession, controller, mediaItems)
    }

    override fun onSetRating(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        rating: androidx.media3.common.Rating,
    ): ListenableFuture<SessionResult> {
        return sessionCallback.onSetRating(session, controller, rating)
    }

    override fun onMediaButtonEvent(
        session: MediaSession,
        controllerInfo: MediaSession.ControllerInfo,
        intent: android.content.Intent,
    ): Boolean {
        return sessionCallback.onMediaButtonEvent(session, controllerInfo, intent)
    }

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val isRecent = params?.isRecent == true
        val isSuggested = params?.isSuggested == true
        val rootId = browseTreeProvider.getRootId(
            isRecent = isRecent,
            isSuggested = isSuggested,
            hasCurrentEpisode = true,
        ) ?: return Futures.immediateFuture(
            LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE),
        )

        val rootItem = MediaItem.Builder()
            .setMediaId(rootId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
    }

    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
        scope.launch {
            try {
                val compatItems = browseTreeProvider.loadChildren(parentId, contextProvider())
                val media3Items = compatItems.map { toMedia3MediaItem(it) }
                val paginated = paginate(media3Items, page, pageSize)
                future.set(LibraryResult.ofItemList(paginated, params))
            } catch (e: Exception) {
                future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN))
            }
        }
        return future
    }

    override fun onGetSearchResult(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
        scope.launch {
            try {
                val compatItems = browseTreeProvider.search(query, contextProvider())
                if (compatItems == null) {
                    future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN))
                } else {
                    val media3Items = compatItems.map { toMedia3MediaItem(it) }
                    val paginated = paginate(media3Items, page, pageSize)
                    future.set(LibraryResult.ofItemList(paginated, params))
                }
            } catch (e: Exception) {
                future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN))
            }
        }
        return future
    }

    override fun onSearch(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> {
        scope.launch {
            browseTreeProvider.search(query, contextProvider())
            session.notifySearchResultChanged(browser, query, 0, params)
        }
        return Futures.immediateFuture(LibraryResult.ofVoid(params))
    }

    private fun paginate(items: List<MediaItem>, page: Int, pageSize: Int): List<MediaItem> {
        if (pageSize <= 0 || page < 0) return items
        val start = (page * pageSize).coerceAtMost(items.size)
        val end = (start + pageSize).coerceAtMost(items.size)
        return items.subList(start, end)
    }

    companion object {
        /**
         * Converts a compat [MediaBrowserCompat.MediaItem] to a Media3 [MediaItem] by extracting
         * mediaId, title, subtitle, iconUri, and flags (browsable/playable) from the
         * [MediaDescriptionCompat][android.support.v4.media.MediaDescriptionCompat].
         */
        internal fun toMedia3MediaItem(compatItem: MediaBrowserCompat.MediaItem): MediaItem {
            val description = compatItem.description
            val isBrowsable = compatItem.flags and MediaBrowserCompat.MediaItem.FLAG_BROWSABLE != 0
            val isPlayable = compatItem.flags and MediaBrowserCompat.MediaItem.FLAG_PLAYABLE != 0

            val metadata = MediaMetadata.Builder()
                .setTitle(description.title)
                .setSubtitle(description.subtitle)
                .setArtworkUri(description.iconUri)
                .setIsBrowsable(isBrowsable)
                .setIsPlayable(isPlayable)
                .build()

            return MediaItem.Builder()
                .setMediaId(description.mediaId.orEmpty())
                .setMediaMetadata(metadata)
                .build()
        }
    }
}
