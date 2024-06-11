package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ChapterIndices
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import java.util.Date
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

    @Transaction
    open suspend fun selectChapter(episodeUuid: String, chapterIndex: Int, select: Boolean, modifiedAt: Date = Date()) {
        val episode = findPodcastEpisode(episodeUuid) ?: findUserEpisode(episodeUuid) ?: return
        val deselectedChapters = episode.deselectedChapters
        episode.deselectedChapters = when {
            select && chapterIndex in deselectedChapters -> ChapterIndices((deselectedChapters - chapterIndex).distinct())
            !select && chapterIndex !in deselectedChapters -> ChapterIndices((deselectedChapters + chapterIndex).distinct())
            else -> return
        }
        episode.deselectedChaptersModified = modifiedAt
        update(episode)
    }

    @Query("SELECT * FROM podcast_episodes WHERE uuid IS :episodeUuid")
    protected abstract suspend fun findPodcastEpisode(episodeUuid: String): PodcastEpisode?

    @Query("SELECT * FROM user_episodes WHERE uuid IS :episodeUuid")
    protected abstract suspend fun findUserEpisode(episodeUuid: String): UserEpisode?

    private suspend fun update(episode: BaseEpisode) = when (episode) {
        is PodcastEpisode -> updatePodcastEpisode(episode)
        is UserEpisode -> updateUserEpisode(episode)
    }

    @Update
    protected abstract suspend fun updatePodcastEpisode(episode: PodcastEpisode)

    @Update
    protected abstract suspend fun updateUserEpisode(episode: UserEpisode)
}
