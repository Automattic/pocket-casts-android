package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.PackageValidator
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * [MediaLibraryService.MediaLibrarySession.Callback] that provides browse tree and search
 * functionality using [BrowseTreeProvider] and delegates session-level operations
 * (onConnect, onCustomCommand, onAddMediaItems, onSetRating, onMediaButtonEvent) to
 * [Media3SessionCallback].
 */
@OptIn(UnstableApi::class)
internal class Media3LibrarySessionCallback(
    private val sessionCallback: Media3SessionCallback,
    private val browseTreeProvider: BrowseTreeProvider,
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
    private val packageValidator: PackageValidator?,
    private val scope: CoroutineScope,
    private val contextProvider: () -> android.content.Context,
) : MediaLibraryService.MediaLibrarySession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        if (packageValidator != null && !packageValidator.isKnownCaller(controller.packageName, controller.uid)) {
            Timber.e("Unknown caller trying to connect to Media3 session: ${controller.packageName} ${controller.uid}")
            return MediaSession.ConnectionResult.reject()
        }
        if (!controller.packageName.contains("au.com.shiftyjelly.pocketcasts")) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Client: ${controller.packageName} connected to media session")
            val context = contextProvider()
            if (Util.isAutomotive(context) && !settings.automotiveConnectedToMediaSession()) {
                scope.launch {
                    try {
                        delay(1000)
                        settings.setAutomotiveConnectedToMediaSession(true)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to set automotive connected flag")
                    }
                }
            }
        }
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

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        playbackHasBeenResumed: Boolean,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
        scope.launch {
            try {
                val episode = playbackManager.getCurrentEpisode()
                if (episode != null) {
                    val mediaItem = MediaItem.Builder().setMediaId(episode.uuid).build()
                    future.set(
                        MediaSession.MediaItemsWithStartPosition(
                            listOf(mediaItem),
                            0,
                            episode.playedUpToMs.toLong(),
                        ),
                    )
                } else {
                    future.setException(UnsupportedOperationException("No episode to resume"))
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
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
            hasCurrentEpisode = playbackManager.getCurrentEpisode() != null,
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

        val extras = Bundle().apply {
            putBoolean(MEDIA_SEARCH_SUPPORTED, true)
            putBoolean(CONTENT_STYLE_SUPPORTED, true)
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID_ITEM_HINT_VALUE)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST_ITEM_HINT_VALUE)
        }
        val responseParams = MediaLibraryService.LibraryParams.Builder()
            .setExtras(extras)
            .build()

        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, responseParams))
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
                val items = browseTreeProvider.loadChildren(parentId, contextProvider())
                val paginated = paginate(items, page, pageSize)
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
                val items = browseTreeProvider.search(query, contextProvider())
                if (items == null) {
                    future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN))
                } else {
                    val paginated = paginate(items, page, pageSize)
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
            try {
                browseTreeProvider.search(query, contextProvider())
                session.notifySearchResultChanged(browser, query, 0, params)
            } catch (e: Exception) {
                Timber.e(e, "Search failed for query: $query")
            }
        }
        return Futures.immediateFuture(LibraryResult.ofVoid(params))
    }

    private fun paginate(items: List<MediaItem>, page: Int, pageSize: Int): List<MediaItem> {
        if (pageSize <= 0 || page < 0) return items
        val start = (page * pageSize).coerceAtMost(items.size)
        val end = (start + pageSize).coerceAtMost(items.size)
        return items.subList(start, end)
    }
}
