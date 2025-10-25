package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextChange

@Dao
abstract class UpNextChangeDao {

    @Query("SELECT * FROM up_next_changes")
    abstract fun findAllBlocking(): List<UpNextChange>

    @Query("DELETE FROM up_next_changes WHERE modified <= :modified")
    abstract suspend fun deleteChangesOlderOrEqualTo(modified: Long)

    @Query("DELETE FROM up_next_changes WHERE uuid = :uuid")
    abstract fun deleteByUuidBlocking(uuid: String)

    @Query("DELETE FROM up_next_changes")
    abstract fun deleteAllBlocking()

    fun savePlayNowBlocking(episode: BaseEpisode) {
        saveUpdateBlocking(episode, UpNextChange.ACTION_PLAY_NOW)
    }

    fun savePlayNextBlocking(episode: BaseEpisode) {
        saveUpdateBlocking(episode, UpNextChange.ACTION_PLAY_NEXT)
    }

    fun savePlayLastBlocking(episode: BaseEpisode) {
        saveUpdateBlocking(episode, UpNextChange.ACTION_PLAY_LAST)
    }

    fun saveRemoveBlocking(episode: BaseEpisode) {
        saveUpdateBlocking(episode, UpNextChange.ACTION_REMOVE)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertBlocking(upNextChange: UpNextChange)

    private fun saveUpdateBlocking(episode: BaseEpisode, action: Int) {
        val change = UpNextChange(type = action, uuid = episode.uuid, modified = System.currentTimeMillis())
        // an update replaces any other update that is for the same episode, so delete any that might exist
        deleteByUuidBlocking(episode.uuid)
        insertBlocking(change)
    }

    @Transaction
    open fun saveReplace(episodeUuids: List<String>) {
        val episodeUuidsString = episodeUuids.joinToString(separator = ",")
        val change = UpNextChange(type = UpNextChange.ACTION_REPLACE, uuids = episodeUuidsString, modified = System.currentTimeMillis())
        // a replace literally replaces everything that came before it, so empty the table out
        deleteAllBlocking()
        insertBlocking(change)
    }
}
