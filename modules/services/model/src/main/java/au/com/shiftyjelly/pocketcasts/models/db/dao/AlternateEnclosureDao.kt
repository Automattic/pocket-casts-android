package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure

@Dao
abstract class AlternateEnclosureDao {
    @Query("SELECT * FROM episode_alternate_enclosures WHERE episode_uuid IS :episodeUuid ORDER BY position ASC")
    abstract suspend fun findByEpisodeUuid(episodeUuid: String): List<EpisodeAlternateEnclosure>

    @Insert
    protected abstract suspend fun insertAll(enclosures: List<EpisodeAlternateEnclosure>)

    @Query("DELETE FROM episode_alternate_enclosures WHERE episode_uuid IS :episodeUuid")
    protected abstract suspend fun deleteForEpisode(episodeUuid: String)

    @Transaction
    open suspend fun replaceForEpisode(episodeUuid: String, enclosures: List<EpisodeAlternateEnclosure>) {
        deleteForEpisode(episodeUuid)
        if (enclosures.isNotEmpty()) {
            insertAll(enclosures)
        }
    }

    @Insert
    protected abstract fun insertAllBlocking(enclosures: List<EpisodeAlternateEnclosure>)

    @Query("DELETE FROM episode_alternate_enclosures WHERE episode_uuid IS :episodeUuid")
    protected abstract fun deleteForEpisodeBlocking(episodeUuid: String)

    @Transaction
    open fun replaceForEpisodeBlocking(episodeUuid: String, enclosures: List<EpisodeAlternateEnclosure>) {
        deleteForEpisodeBlocking(episodeUuid)
        if (enclosures.isNotEmpty()) {
            insertAllBlocking(enclosures)
        }
    }
}
