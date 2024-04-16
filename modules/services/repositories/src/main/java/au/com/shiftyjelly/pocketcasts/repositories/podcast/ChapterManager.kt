package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import kotlinx.coroutines.flow.Flow
import au.com.shiftyjelly.pocketcasts.models.to.DbChapter as Chapter

interface ChapterManager {
    suspend fun updateChapters(
        episodeUuid: String,
        chapters: List<Chapter>,
        forceUpdate: Boolean = true,
    )

    fun observerChaptersForEpisode(episodeUuid: String): Flow<Chapters>
}
