package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.dao.ChapterDao
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.ChapterOrigin
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.DbChapter
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.Lazy
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class ChapterManagerImpl @Inject constructor(
    private val chapterDao: ChapterDao,
    private val episodeManager: EpisodeManager,
    private val fingerprintTimingManager: Lazy<FingerprintTimingManager>,
    private val appPlatform: AppPlatform,
) : ChapterManager {
    private var unalignedLogEpisodeUuid: String? = null

    override suspend fun updateChapters(
        episodeUuid: String,
        chapters: List<DbChapter>,
    ) {
        chapterDao.replaceAllChapters(episodeUuid, chapters)
    }

    override suspend fun selectChapter(episodeUuid: String, chapterIndex: Int, select: Boolean) {
        chapterDao.selectChapter(episodeUuid, chapterIndex, select)
    }

    override suspend fun hasChapters(episodeUuid: String) = chapterDao.countForEpisode(episodeUuid) > 0

    override suspend fun hasGeneratedChapters(episodeUuid: String) = chapterDao.countGeneratedForEpisode(episodeUuid) > 0

    @OptIn(FlowPreview::class)
    override fun observerChaptersForEpisode(episodeUuid: String): Flow<Chapters> {
        val rawChapters = combine(
            episodeManager.findEpisodeByUuidFlow(episodeUuid).distinctUntilChangedBy(BaseEpisode::deselectedChapters),
            chapterDao.observeChaptersForEpisode(episodeUuid),
        ) { episode, dbChapters ->
            // Already-saved generated chapters must stay hidden while the feature is off.
            val visibleChapters = if (FeatureFlag.isEnabled(Feature.GENERATED_CHAPTERS)) {
                dbChapters
            } else {
                dbChapters.filterNot { it.origin == ChapterOrigin.Generated }
            }
            Chapters(visibleChapters.fixChapterTimestamps(episode))
        }

        // Generated chapter timestamps are in the server reference timeline; align them to the real audio
        // stream via the fingerprint map. Phone only, to spare resources on automotive/wear.
        if (appPlatform != AppPlatform.Phone) return rawChapters

        // -1L primes combine so chapters emit immediately, before sample's first throttled tick.
        val mappingTicks = fingerprintTimingManager.get().mappingVersion
            .sample(MAPPING_SAMPLE_MS)
            .onStart { emit(-1L) }
        return combine(rawChapters, mappingTicks) { chapters, _ ->
            alignGeneratedChapters(episodeUuid, chapters)
        }.distinctUntilChanged()
    }

    /**
     * Translate generated chapters from reference time to stream time using the fingerprint map.
     * Embedded chapters already match the file, so they are left untouched.
     */
    private fun alignGeneratedChapters(episodeUuid: String, chapters: Chapters): Chapters {
        val manager = fingerprintTimingManager.get()
        if (manager.activeEpisodeUuid != episodeUuid || manager.mappingSnapshot.isEmpty()) {
            if (chapters.hasGeneratedChapters && unalignedLogEpisodeUuid != episodeUuid) {
                unalignedLogEpisodeUuid = episodeUuid
                LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Generated chapters for $episodeUuid are unaligned: no fingerprint mapping available")
            }
            return chapters
        }
        return alignGeneratedChapters(chapters) { reference ->
            manager.playbackTimeMs(reference.toDouble(DurationUnit.SECONDS))?.milliseconds
        }
    }

    private fun List<DbChapter>.fixChapterTimestamps(episode: BaseEpisode) = asSequence()
        .withIndex()
        .windowed(size = 2, partialWindows = true) { window ->
            val sequenceIndex = window[0].index
            val firstChapter = window[0].value
            val secondChapter = window.getOrNull(1)?.value

            val newStartTime = if (sequenceIndex == 0 && firstChapter.origin != ChapterOrigin.Generated) Duration.ZERO else firstChapter.startTimeMs.milliseconds
            val secondStartTime = secondChapter?.startTimeMs?.milliseconds ?: episode.durationMs.milliseconds
            val newEndTime = firstChapter.endTimeMs?.milliseconds?.takeIf { it <= secondStartTime && it > newStartTime } ?: secondStartTime

            Chapter(
                title = firstChapter.title.orEmpty(),
                startTime = newStartTime,
                endTime = newEndTime,
                url = firstChapter.url?.toHttpUrlOrNull(),
                imagePath = firstChapter.imageUrl,
                index = firstChapter.index,
                uiIndex = -1, // We set any value here as it is updated later in the processing chain
                selected = firstChapter.index !in episode.deselectedChapters,
                origin = firstChapter.origin,
            )
        }
        .filter { it.duration > Duration.ZERO }
        .mapIndexed { index, chapter -> chapter.copy(uiIndex = index + 1) }
        .toList()

    companion object {
        private const val MAPPING_SAMPLE_MS = 1000L

        /**
         * Translate generated chapters from reference time to stream time with [align]. Embedded chapters
         * already match the file, so they are left untouched; zero-duration chapters are dropped and the
         * UI index re-derived, mirroring [fixChapterTimestamps].
         */
        internal fun alignGeneratedChapters(chapters: Chapters, align: (Duration) -> Duration?): Chapters {
            if (chapters.none { it.isGenerated }) return chapters
            val aligned = chapters
                .map { chapter ->
                    if (!chapter.isGenerated) {
                        chapter
                    } else {
                        chapter.copy(
                            startTime = align(chapter.startTime) ?: chapter.startTime,
                            endTime = align(chapter.endTime) ?: chapter.endTime,
                        )
                    }
                }
                .filter { it.duration > Duration.ZERO }
                .mapIndexed { index, chapter -> chapter.copy(uiIndex = index + 1) }
            return Chapters(aligned)
        }
    }
}
