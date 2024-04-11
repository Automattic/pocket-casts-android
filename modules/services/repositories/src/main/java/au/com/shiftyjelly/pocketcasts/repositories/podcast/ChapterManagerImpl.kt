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
        forceUpdate: Boolean,
    ) {
        if (forceUpdate) {
            chapterDao.replaceAllChapters(episodeUuid, chapters)
        } else {
            chapterDao.replaceAllChaptersIfMoreIsPassed(episodeUuid, chapters)
        }
    }

    override fun observerChaptersForEpisode(episodeUuid: String) = combine(
        episodeManager.observeEpisodeByUuid(episodeUuid).distinctUntilChangedBy(BaseEpisode::deselectedChapters),
        chapterDao.observerChaptersForEpisode(episodeUuid),
    ) { episode, dbChapters -> dbChapters.toChapters(episode) }

    private fun List<DbChapter>.toChapters(episode: BaseEpisode): Chapters {
        val chaptersList = withIndex().windowed(size = 2, partialWindows = true) { window ->
            val index = window[0].index
            val chapterIndex = window[0].index + 1
            val firstChapter = window[0].value
            val secondChapter = window.getOrNull(1)?.value

            val secondEndTime = secondChapter?.startTimeMs?.milliseconds ?: episode.durationMs.milliseconds
            val fixedEndTime = firstChapter.endTimeMs?.milliseconds?.takeIf { it <= secondEndTime } ?: secondEndTime

            Chapter(
                title = firstChapter.title.orEmpty(),
                startTime = if (index == 0) Duration.ZERO else firstChapter.startTimeMs.milliseconds,
                endTime = fixedEndTime,
                url = firstChapter.url?.toHttpUrlOrNull(),
                imagePath = firstChapter.imageUrl,
                index = chapterIndex,
                selected = chapterIndex !in episode.deselectedChapters,
            )
        }
        return Chapters(chaptersList)
    }
}
