package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadProgressCache
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.containsUuid
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.rx2.asFlow

data class EpisodeRowData(
    val downloadProgress: Int,
    val playbackState: PlaybackState,
    val isInUpNext: Boolean,
    val hasBookmarks: Boolean,
    val hasHlsAlternateEnclosure: Boolean,
)

data class UserEpisodeRowData(
    val episode: UserEpisode,
    val downloadProgress: Int,
    val uploadProgress: Int,
    val playbackState: PlaybackState,
    val isInUpNext: Boolean,
    val hasBookmarks: Boolean,
)

class EpisodeRowDataProvider @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val downloadProgressCache: DownloadProgressCache,
    private val playbackManager: PlaybackManager,
    private val upNextQueue: UpNextQueue,
    private val bookmarkManager: BookmarkManager,
    private val userEpisodeManager: UserEpisodeManager,
    private val alternateEnclosureManager: AlternateEnclosureManager,
    private val settings: Settings,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /** Collect on the main dispatcher, the row data is bound straight into views. */
    fun userEpisodeRowDataFlow(episodeUuid: String): Flow<UserEpisodeRowData> {
        // combine has no typed overload for six sources, hence the nested combine for the two flags
        return combine(
            userEpisodeManager.episodeFlow(episodeUuid).filterNotNull(),
            downloadProgressFlow(episodeUuid),
            uploadProgressFlow(episodeUuid),
            playbackStatusFlow(episodeUuid),
            combine(isInUpNextFlow(episodeUuid), hasBookmarksFlow(episodeUuid), ::Pair),
        ) { episode, downloadProgress, uploadProgress, playbackState, (isInUpNext, hasBookmarks) ->
            UserEpisodeRowData(
                episode = episode,
                downloadProgress = downloadProgress,
                uploadProgress = uploadProgress,
                playbackState = playbackState,
                isInUpNext = isInUpNext,
                hasBookmarks = hasBookmarks,
            )
        }.flowOn(ioDispatcher)
    }

    /** Collect on the main dispatcher, the row data is bound straight into views. */
    fun episodeRowDataFlow(episodeUuid: String): Flow<EpisodeRowData> {
        return flow {
            // an episode that is not in the database has no row data, so emit nothing
            if (episodeManager.findEpisodeByUuid(episodeUuid) == null) {
                return@flow
            }
            emitAll(
                combine(
                    downloadProgressFlow(episodeUuid),
                    playbackStatusFlow(episodeUuid),
                    isInUpNextFlow(episodeUuid),
                    hasBookmarksFlow(episodeUuid),
                    hasHlsAlternateEnclosureFlow(episodeUuid),
                    ::EpisodeRowData,
                ),
            )
        }.flowOn(ioDispatcher)
    }

    private fun downloadProgressFlow(episodeUuid: String): Flow<Int> {
        return downloadProgressCache.progressFlow(episodeUuid)
            .map { progress -> progress?.percentage ?: 0 }
            .distinctUntilChanged()
    }

    private fun uploadProgressFlow(episodeUuid: String): Flow<Int> {
        return UploadProgressManager.progressFlow(episodeUuid)
            .map { (it * 100).roundToInt() }
            .throttleLatest(1.seconds)
            .onStart { emit(0) }
            .distinctUntilChanged()
    }

    private fun playbackStatusFlow(episodeUuid: String): Flow<PlaybackState> {
        val emptyState = PlaybackState(episodeUuid = episodeUuid)
        return playbackManager.playbackStateFlow
            .onStart { emit(emptyState) }
            .map { if (it.episodeUuid == episodeUuid) it else emptyState }
            .distinctUntilChanged { previous, current ->
                previous.state == current.state &&
                    previous.episodeUuid == current.episodeUuid &&
                    previous.positionMs == current.positionMs &&
                    previous.isBuffering == current.isBuffering
            }
    }

    private fun hasHlsAlternateEnclosureFlow(episodeUuid: String): Flow<Boolean> {
        return alternateEnclosureManager.hasHlsAlternateEnclosure(episodeUuid)
            .onStart { emit(false) }
            .distinctUntilChanged()
    }

    private fun isInUpNextFlow(episodeUuid: String): Flow<Boolean> {
        return upNextQueue.changesObservable
            .asFlow()
            .containsUuid(episodeUuid)
            .onStart { emit(false) }
            .distinctUntilChanged()
    }

    private fun hasBookmarksFlow(episodeUuid: String): Flow<Boolean> {
        fun hasActiveBookmarks(subscription: Subscription?, hasBookmarks: Boolean): Boolean {
            return hasBookmarks && subscription != null
        }

        return combine(
            settings.cachedSubscription.flow,
            bookmarkManager.hasBookmarksFlow(episodeUuid),
            ::hasActiveBookmarks,
        )
            .onStart { emit(false) }
            .distinctUntilChanged()
    }
}

// emits the first value straight away, then at most one value per window, always the latest one
private fun <T> Flow<T>.throttleLatest(window: Duration): Flow<T> = flow {
    conflate().collect { value ->
        emit(value)
        delay(window)
    }
}
