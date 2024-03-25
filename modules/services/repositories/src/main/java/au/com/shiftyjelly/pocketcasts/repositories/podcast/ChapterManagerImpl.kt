package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.dao.ChapterDao
import au.com.shiftyjelly.pocketcasts.models.to.DbChapter
import javax.inject.Inject

class ChapterManagerImpl @Inject constructor(
    private val chapterDao: ChapterDao,
) : ChapterManager {
    override suspend fun updateChapters(episodeUuid: String, chapters: List<DbChapter>) {
        chapterDao.replaceAllChapters(episodeUuid, chapters)
    }
}
