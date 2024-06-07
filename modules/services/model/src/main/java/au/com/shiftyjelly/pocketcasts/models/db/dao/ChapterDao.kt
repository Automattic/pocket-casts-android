package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import au.com.shiftyjelly.pocketcasts.models.to.DbChapter as Chapter

@Dao
abstract class ChapterDao {
    @Query("SELECT * FROM episode_chapters")
    abstract suspend fun findAll(): List<Chapter>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(chapters: List<Chapter>)

    @Query("DELETE FROM episode_chapters WHERE episode_uuid IS :episodeUuid")
    protected abstract suspend fun deleteForEpisode(episodeUuid: String)

    @Transaction
    open suspend fun replaceAllChapters(episodeUuid: String, chapters: List<Chapter>) {
        val newEpisodeChapters = chapters.filter { it.episodeUuid == episodeUuid }
        if (newEpisodeChapters.isEmpty()) {
            return
        }

        when {
            newEpisodeChapters.all(Chapter::isEmbedded) -> {
                deleteForEpisode(episodeUuid)
                insertAll(newEpisodeChapters)
            }
            findEpisodeChapters(episodeUuid).let { currentChapters -> currentChapters.size <= chapters.size && currentChapters.none(Chapter::isEmbedded) } -> {
                deleteForEpisode(episodeUuid)
                insertAll(newEpisodeChapters)
            }
        }
    }

    @Query("SELECT * FROM episode_chapters WHERE episode_uuid IS :episodeUuid ORDER BY start_time ASC")
    protected abstract suspend fun findEpisodeChapters(episodeUuid: String): List<Chapter>

    @Query("SELECT * FROM episode_chapters WHERE episode_uuid IS :episodeUuid ORDER BY start_time ASC")
    protected abstract fun _observerChaptersForEpisode(episodeUuid: String): Flow<List<Chapter>>

    fun observerChaptersForEpisode(episodeUuid: String) = _observerChaptersForEpisode(episodeUuid).distinctUntilChanged()
}
