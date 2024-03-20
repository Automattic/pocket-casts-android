package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.to.DbChapter as Chapter

interface ChapterManager {
    suspend fun updateChapters(episodeUuid: String, chapters: List<Chapter>)
}
