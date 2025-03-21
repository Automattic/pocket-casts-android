package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextHistoryEntry
import java.util.Date

@Dao
abstract class UpNextHistoryDao {
    @Transaction
    @Query(
        """
        INSERT OR IGNORE INTO up_next_history(
            episodeUuid,
            position,
            playlistId,
            title,
            publishedDate,
            downloadUrl,
            podcastUuid,
            addedDate
        )
        SELECT 
            episodeUuid,
            position,
            playlistId,
            title,
            publishedDate,
            downloadUrl,
            podcastUuid,
            :date
        FROM up_next_episodes
    """,
    )
    abstract suspend fun insertHistoryForDate(date: Date)

    @Query(
        """
        SELECT 
            addedDate as date,
            COUNT(*) as episodeCount 
        FROM up_next_history 
        GROUP BY addedDate 
        ORDER BY addedDate DESC
    """,
    )
    abstract suspend fun findAllHistoryEntries(): List<UpNextHistoryEntry>

    @Query(
        """
        SELECT episodeUuid FROM up_next_history 
        WHERE addedDate = :date
        ORDER BY position ASC
    """,
    )
    abstract suspend fun findEpisodeUuidsForDate(date: Date): List<String>

    @Query(
        """
        DELETE FROM up_next_history 
        WHERE addedDate <= :date
    """,
    )
    abstract suspend fun deleteHistoryOnOrBeforeDate(date: Date)
}
