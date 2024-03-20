package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.dao.ChapterDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.DbChapter
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class ChapterManagerImpl @Inject constructor(
    private val chapterDao: ChapterDao,
    private val episodeDao: EpisodeDao,
) : ChapterManager {
    override suspend fun updateChapters(episodeUuid: String, chapters: List<DbChapter>) {
        chapterDao.replaceAllChapters(episodeUuid, chapters)
    }

    override fun observerChaptersForEpisode(episodeUuid: String) = chapterDao
        .observerChaptersForEpisode(episodeUuid)
        .map { it.toChapters(episodeUuid) }

    private suspend fun List<DbChapter>.toChapters(episodeUuid: String): Chapters {
        val deselectedChapters = episodeDao.findByUuid(episodeUuid)?.deselectedChapters.orEmpty()
        val chaptersList =  withIndex().windowed(size = 2, partialWindows = true) { window ->
            val index = window[0].index + 1
            val firstChapter = window[0].value
            val secondChapter = window.getOrNull(1)?.value

            val secondEndTime = secondChapter?.startTimeMs?.milliseconds ?: Duration.INFINITE
            val fixedEndTime = firstChapter.endTimeMs?.milliseconds?.takeIf { it <= secondEndTime } ?: secondEndTime

            Chapter(
                title = firstChapter.title.orEmpty(),
                startTime = firstChapter.startTimeMs.milliseconds,
                endTime = fixedEndTime,
                url = firstChapter.url?.toHttpUrlOrNull(),
                imagePath = firstChapter.imageUrl,
                index = index,
                selected = index !in deselectedChapters,
            )
        }
        return Chapters(chaptersList)
    }
}
