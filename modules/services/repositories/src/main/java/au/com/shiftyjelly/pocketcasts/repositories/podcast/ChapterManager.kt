package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.DbChapter
import kotlinx.coroutines.flow.Flow

interface ChapterManager {
    suspend fun updateChapters(episodeUuid: String, chapters: List<DbChapter>)

    fun observerChaptersForEpisode(episodeUuid: String): Flow<Chapters>
}
