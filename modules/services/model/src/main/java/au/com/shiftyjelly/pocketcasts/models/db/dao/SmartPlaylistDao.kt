package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import io.reactivex.Flowable
import io.reactivex.Maybe
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SmartPlaylistDao {

    @Query("SELECT * FROM playlists WHERE _id = :id")
    abstract fun findByIdBlocking(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract suspend fun findAll(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun findAllRxFlowable(): Flowable<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE uuid = :uuid LIMIT 1")
    abstract suspend fun findByUuid(uuid: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE uuid = :uuid LIMIT 1")
    abstract fun findByUuidRxMaybe(uuid: String): Maybe<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE uuid = :uuid LIMIT 1")
    abstract fun findByUuidRxFlowable(uuid: String): Flowable<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE uuid = :uuid")
    abstract fun findByUuidAsListRxFlowable(uuid: String): Flowable<List<PlaylistEntity>>

    @Query("SELECT COUNT(*) FROM playlists")
    abstract fun countBlocking(): Int

    @Update
    abstract suspend fun update(playlist: PlaylistEntity)

    @Update
    abstract fun updateBlocking(playlist: PlaylistEntity)

    @Update
    abstract fun updateAllBlocking(playlists: List<PlaylistEntity>)

    @Delete
    abstract fun deleteBlocking(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertBlocking(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET syncStatus = :syncStatus WHERE uuid = :uuid")
    abstract fun updateSyncStatusBlocking(syncStatus: Int, uuid: String)
}
