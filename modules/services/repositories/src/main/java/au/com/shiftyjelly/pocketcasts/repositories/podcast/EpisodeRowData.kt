package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.containsUuid
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.rx2.asObservable

data class EpisodeRowData(
    val downloadProgress: Int,
    val uploadProgress: Int,
    val playbackState: PlaybackState,
    val isInUpNext: Boolean,
    val hasBookmarks: Boolean,
)

class EpisodeRowDataProvider(
    private val episodeManager: EpisodeManager,
    private val downloadManager: DownloadManager,
    private val playbackManager: PlaybackManager,
    private val upNextQueue: UpNextQueue,
    private val bookmarkManager: BookmarkManager,
    private val settings: Settings,
) {
    fun episodeRowDataObservable(episodeUuid: String): Observable<EpisodeRowData> {
        return episodeManager.findEpisodeByUuidRxFlowable(episodeUuid)
            .toObservable()
            .flatMap { episode ->
                Observables.combineLatest(
                    downloadProgressObservable(episode),
                    uploadProgressObservable(episode),
                    playbackStatusObservable(episode),
                    isInUpNextObservable(episode),
                    hasBookmarksObservable(episode),
                    ::EpisodeRowData,
                )
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun downloadProgressObservable(episode: BaseEpisode): Observable<Int> {
        return downloadManager.progressUpdateRelay
            .filter { it.episodeUuid == episode.uuid }
            .map { (it.downloadProgress * 100).roundToInt() }
            .throttleLatest(1, TimeUnit.SECONDS)
            .startWith(0)
            .distinctUntilChanged()
    }

    private fun uploadProgressObservable(episode: BaseEpisode): Observable<Int> {
        val relay = BehaviorRelay.create<Float>()
        return relay
            .doOnSubscribe { UploadProgressManager.observeUploadProgress(episode.uuid, relay) }
            .doOnDispose { UploadProgressManager.stopObservingUpload(episode.uuid, relay) }
            .map { (it * 100).roundToInt() }
            .throttleLatest(1, TimeUnit.SECONDS)
            .startWith(0)
            .distinctUntilChanged()
    }

    private fun playbackStatusObservable(episode: BaseEpisode): Observable<PlaybackState> {
        val emptyState = PlaybackState(episodeUuid = episode.uuid)
        return playbackManager.playbackStateRelay
            .startWith(emptyState)
            .map { if (it.episodeUuid == episode.uuid) it else emptyState }
            .distinctUntilChanged()
    }

    private fun isInUpNextObservable(episode: BaseEpisode): Observable<Boolean> {
        return upNextQueue
            .changesObservable
            .containsUuid(episode.uuid)
            .startWith(false)
            .distinctUntilChanged()
    }

    private fun hasBookmarksObservable(episode: BaseEpisode): Observable<Boolean> {
        fun hasActiveBookmarks(subscription: Subscription?, hasBookmarks: Boolean): Boolean {
            return hasBookmarks && subscription != null
        }

        val combinedDataFlow = combine(
            settings.cachedSubscription.flow,
            bookmarkManager.hasBookmarksFlow(episode.uuid),
            ::hasActiveBookmarks,
        )

        return combinedDataFlow
            .asObservable()
            .startWith(false)
            .distinctUntilChanged()
    }
}
