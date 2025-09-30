package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.containsUuid
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.rx2.asObservable
import kotlinx.coroutines.rx2.rxMaybe

data class EpisodeRowData(
    val downloadProgress: Int,
    val uploadProgress: Int,
    val playbackState: PlaybackState,
    val isInUpNext: Boolean,
    val hasBookmarks: Boolean,
)

class EpisodeRowDataProvider @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val downloadManager: DownloadManager,
    private val playbackManager: PlaybackManager,
    private val upNextQueue: UpNextQueue,
    private val bookmarkManager: BookmarkManager,
    private val settings: Settings,
) {
    fun episodeRowDataObservable(episodeUuid: String): Observable<EpisodeRowData> {
        return rxMaybe { episodeManager.findEpisodeByUuid(episodeUuid) }
            .toObservable()
            .flatMap { episode ->
                Observables.combineLatest(
                    downloadProgressObservable(episodeUuid),
                    when (episode) {
                        is PodcastEpisode -> Observable.just(0)
                        is UserEpisode -> uploadProgressObservable(episodeUuid)
                    },
                    playbackStatusObservable(episodeUuid),
                    isInUpNextObservable(episodeUuid),
                    hasBookmarksObservable(episodeUuid),
                    ::EpisodeRowData,
                )
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun downloadProgressObservable(episodeUuid: String): Observable<Int> {
        return downloadManager.episodeDownloadProgressFlow(episodeUuid)
            .asObservable()
            .map { (it.downloadProgress * 100).roundToInt() }
            .throttleLatest(1, TimeUnit.SECONDS)
            .startWith(0)
            .distinctUntilChanged()
    }

    private fun uploadProgressObservable(episodeUuid: String): Observable<Int> {
        return UploadProgressManager.progressFlow(episodeUuid)
            .asObservable()
            .map { (it * 100).roundToInt() }
            .throttleLatest(1, TimeUnit.SECONDS)
            .startWith(0)
            .distinctUntilChanged()
    }

    private fun playbackStatusObservable(episodeUuid: String): Observable<PlaybackState> {
        val emptyState = PlaybackState(episodeUuid = episodeUuid)
        return playbackManager.playbackStateRelay
            .startWith(emptyState)
            .map { if (it.episodeUuid == episodeUuid) it else emptyState }
            .distinctUntilChanged()
    }

    private fun isInUpNextObservable(episodeUuid: String): Observable<Boolean> {
        return upNextQueue
            .changesObservable
            .containsUuid(episodeUuid)
            .startWith(false)
            .distinctUntilChanged()
    }

    private fun hasBookmarksObservable(episodeUuid: String): Observable<Boolean> {
        fun hasActiveBookmarks(subscription: Subscription?, hasBookmarks: Boolean): Boolean {
            return hasBookmarks && subscription != null
        }

        val combinedDataFlow = combine(
            settings.cachedSubscription.flow,
            bookmarkManager.hasBookmarksFlow(episodeUuid),
            ::hasActiveBookmarks,
        )

        return combinedDataFlow
            .asObservable()
            .startWith(false)
            .distinctUntilChanged()
    }
}
