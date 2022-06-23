package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextChange
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
abstract class UpNextChangeDao {

    @Query("SELECT * FROM up_next_changes")
    abstract fun findAll(): List<UpNextChange>

    @Query("SELECT * FROM up_next_changes")
    abstract fun findAllRx(): Single<List<UpNextChange>>

    @Query("SELECT * FROM up_next_changes")
    abstract fun observeAll(): Flowable<List<UpNextChange>>

    @Query("DELETE FROM up_next_changes WHERE modified <= :modified")
    abstract fun deleteChangesOlderOrEqualTo(modified: Long)

    fun deleteChangesOlderOrEqualToRx(modified: Long): Completable {
        return Completable.fromAction { deleteChangesOlderOrEqualTo(modified) }
    }

    @Query("DELETE FROM up_next_changes WHERE uuid = :uuid")
    abstract fun deleteByUuid(uuid: String)

    @Query("DELETE FROM up_next_changes")
    abstract fun deleteAll()

    fun savePlayNow(episode: Playable) {
        saveUpdate(episode, UpNextChange.ACTION_PLAY_NOW)
    }

    fun savePlayNext(episode: Playable) {
        saveUpdate(episode, UpNextChange.ACTION_PLAY_NEXT)
    }

    fun savePlayLast(episode: Playable) {
        saveUpdate(episode, UpNextChange.ACTION_PLAY_LAST)
    }

    fun saveRemove(episode: Playable) {
        saveUpdate(episode, UpNextChange.ACTION_REMOVE)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(upNextChange: UpNextChange)

    private fun saveUpdate(episode: Playable, action: Int) {
        val change = UpNextChange(type = action, uuid = episode.uuid, modified = System.currentTimeMillis())
        // an update replaces any other update that is for the same episode, so delete any that might exist
        deleteByUuid(episode.uuid)
        insert(change)
    }

    fun saveReplace(episodeUuids: List<String>) {
        val episodeUuidsString = episodeUuids.joinToString(separator = ",")
        val change = UpNextChange(type = UpNextChange.ACTION_REPLACE, uuids = episodeUuidsString, modified = System.currentTimeMillis())
        // a replace literally replaces everything that came before it, so empty the table out
        deleteAll()
        insert(change)
    }
}
