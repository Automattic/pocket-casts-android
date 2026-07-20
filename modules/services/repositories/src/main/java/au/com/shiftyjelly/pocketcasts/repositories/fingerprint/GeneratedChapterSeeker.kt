package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Resolves generated chapter seeks to the real stream position via the fingerprint map.
 * Returns null whenever the caller should fall back to the chapter's own start time.
 */
@Singleton
class GeneratedChapterSeeker @Inject constructor(
    private val fingerprintTimingManager: Lazy<FingerprintTimingManager>,
    private val appPlatform: AppPlatform,
) {
    data class ResolvingChapter(val episodeUuid: String, val chapterIndex: Int)

    private val mutex = Mutex()

    @Volatile
    private var activeCaller: Job? = null

    // Session cache of resolved chapter starts for the most recent episode.
    private var cacheEpisodeUuid: String? = null
    private val resolvedCache = mutableMapOf<Int, Duration>()

    private val _resolvingChapter = MutableStateFlow<ResolvingChapter?>(null)
    val resolvingChapter: StateFlow<ResolvingChapter?> = _resolvingChapter.asStateFlow()

    fun resolvingChapterIndex(episodeUuid: Flow<String?>): Flow<Int?> = combine(
        resolvingChapter,
        episodeUuid.distinctUntilChanged(),
    ) { resolving, uuid ->
        resolving?.takeIf { it.episodeUuid == uuid }?.chapterIndex
    }

    /** Called when the user seeks by other means, so a stale chapter resolve can't yank playback later. */
    fun cancelActiveResolve() {
        activeCaller?.cancel()
    }

    suspend fun resolveSeekTime(episode: BaseEpisode, chapter: Chapter): Duration? {
        if (!isEnabled(chapter)) return null
        val referenceTime = chapter.referenceStartTime ?: return null

        mutex.withLock {
            if (cacheEpisodeUuid != episode.uuid) {
                cacheEpisodeUuid = episode.uuid
                resolvedCache.clear()
            }
            resolvedCache[chapter.index]
        }?.let { return it }

        val manager = fingerprintTimingManager.get()
        manager.densePlaybackTime(episode.uuid, referenceTime)?.let { return it }

        // Last tap wins: a new resolve cancels the previous caller, so a superseded tap never seeks.
        val myJob = currentCoroutineContext().job
        val resolving = ResolvingChapter(episode.uuid, chapter.index)
        mutex.withLock {
            activeCaller?.takeIf { it !== myJob }?.cancel()
            activeCaller = myJob
            _resolvingChapter.value = resolving
        }
        try {
            return when (val result = manager.resolvePlaybackTime(episode, referenceTime)) {
                is ChapterSeekResult.Resolved -> {
                    val playbackTime = ceil(result.playbackTime.toDouble(DurationUnit.SECONDS)).seconds
                    mutex.withLock {
                        if (cacheEpisodeUuid == episode.uuid) resolvedCache[chapter.index] = playbackTime
                    }
                    Timber.d("GeneratedChapterSeeker: resolved chapter ${chapter.index} to $playbackTime (usedPrior=${result.usedPrior})")
                    playbackTime
                }

                is ChapterSeekResult.Unresolved -> {
                    Timber.d("GeneratedChapterSeeker: unresolved chapter ${chapter.index} (${result.reason})")
                    null
                }
            }
        } finally {
            // The resolving state is only cleared by its owner, so a superseded tap can't wipe the new tap's spinner.
            withContext(NonCancellable) {
                mutex.withLock {
                    if (activeCaller === myJob) {
                        activeCaller = null
                        _resolvingChapter.value = null
                    }
                }
            }
        }
    }

    private fun isEnabled(chapter: Chapter): Boolean = chapter.isGenerated &&
        appPlatform == AppPlatform.Phone &&
        FeatureFlag.isEnabled(Feature.SYNCED_TRANSCRIPTS) &&
        FeatureFlag.isEnabled(Feature.GENERATED_CHAPTERS)
}
