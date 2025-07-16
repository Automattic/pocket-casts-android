package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.dao.ChapterDao
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.DbChapter
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class ChapterManagerImpl @Inject constructor(
    private val chapterDao: ChapterDao,
    private val episodeManager: EpisodeManager,
) : ChapterManager {
    override suspend fun updateChapters(
        episodeUuid: String,
        chapters: List<DbChapter>,
    ) {
        chapterDao.replaceAllChapters(episodeUuid, chapters)
    }

    override suspend fun selectChapter(episodeUuid: String, chapterIndex: Int, select: Boolean) {
        chapterDao.selectChapter(episodeUuid, chapterIndex, select)
    }

    override fun observerChaptersForEpisode(episodeUuid: String) = combine(
        episodeManager.findEpisodeByUuidFlow(episodeUuid).distinctUntilChangedBy(BaseEpisode::deselectedChapters),
        chapterDao.observeChaptersForEpisode(episodeUuid),
    ) { episode, dbChapters -> Chapters(dbChapters.fixChapterTimestamps(episode)) }

    private fun List<DbChapter>.fixChapterTimestamps(episode: BaseEpisode) = asSequence()
        .withIndex()
        .windowed(size = 2, partialWindows = true) { window ->
            val sequenceIndex = window[0].index
            val firstChapter = window[0].value
            val secondChapter = window.getOrNull(1)?.value

            val newStartTime = if (sequenceIndex == 0) Duration.ZERO else firstChapter.startTimeMs.milliseconds
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
            )
        }
        .filter { it.duration > Duration.ZERO }
        .mapIndexed { index, chapter -> chapter.copy(uiIndex = index + 1) }
        .toList()
}
