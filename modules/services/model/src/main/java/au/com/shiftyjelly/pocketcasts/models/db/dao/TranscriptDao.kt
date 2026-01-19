package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import au.com.shiftyjelly.pocketcasts.models.entity.Transcript
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TranscriptDao {
    @Query("DELETE FROM episode_transcript WHERE episode_uuid IN (:episodeUuids)")
    protected abstract suspend fun deleteAll(episodeUuids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(transcripts: List<Transcript>)

    @Transaction
    open suspend fun replaceAll(transcripts: List<Transcript>) {
        deleteAll(transcripts.map { it.episodeUuid })
        insertAll(transcripts)
    }

    @Query("DELETE FROM episode_transcript WHERE episode_uuid = :episodeUuid")
    abstract suspend fun deleteForEpisode(episodeUuid: String)

    @Query("DELETE FROM episode_transcript WHERE episode_uuid IN (:episodeUuids)")
    abstract suspend fun deleteForEpisodes(episodeUuids: List<String>)

    @Query("SELECT * FROM episode_transcript WHERE episode_uuid IS :episodeUuid")
    abstract fun observeTranscripts(episodeUuid: String): Flow<List<Transcript>>
}
