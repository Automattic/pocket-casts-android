package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import au.com.shiftyjelly.pocketcasts.models.to.Transcript

@Dao
abstract class TranscriptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(transcript: Transcript)

    @Query("DELETE FROM episode_transcript WHERE episode_uuid IS :episodeUuid")
    abstract suspend fun deleteForEpisode(episodeUuid: String)
}
