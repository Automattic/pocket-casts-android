package au.com.shiftyjelly.pocketcasts.wear.data.service.playback

import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.android.horologist.media3.logging.ErrorReporter
import com.google.android.horologist.media3.service.SuspendingMediaLibrarySessionCallback
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.CoroutineScope

class MediaLibrarySessionCallback(
    serviceScope: CoroutineScope,
    appEventLogger: ErrorReporter
) : SuspendingMediaLibrarySessionCallback(serviceScope, appEventLogger) {
    override suspend fun onGetLibraryRootInternal(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): LibraryResult<MediaItem> {
        // TODO implement
        return LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
    }

    override suspend fun onGetItemInternal(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): LibraryResult<MediaItem> {
        // TODO implement
        return LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
    }

    override suspend fun onGetChildrenInternal(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): LibraryResult<ImmutableList<MediaItem>> {
        // TODO implement
        return LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
    }
}
