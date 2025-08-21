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
    abstract fun findAllBlocking(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract suspend fun findAll(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun findAllFlow(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun findAllRxFlowable(): Flowable<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE uuid = :uuid LIMIT 1")
    abstract fun findByUuidBlocking(uuid: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE uuid = :uuid LIMIT 1")
    abstract suspend fun findByUuid(uuid: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE uuid = :uuid LIMIT 1")
    abstract fun findByUuidRxMaybe(uuid: String): Maybe<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE uuid = :uuid LIMIT 1")
    abstract fun findByUuidRxFlowable(uuid: String): Flowable<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE uuid = :uuid")
    abstract fun findByUuidAsListRxFlowable(uuid: String): Flowable<List<PlaylistEntity>>

    @Query("SELECT COUNT(*) FROM playlists")
    abstract suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM playlists")
    abstract fun countBlocking(): Int

    @Update
    abstract suspend fun update(playlist: PlaylistEntity)

    @Update
    abstract fun updateBlocking(playlist: PlaylistEntity)

    @Update
    abstract fun updateAllBlocking(playlists: List<PlaylistEntity>)

    @Delete
    abstract suspend fun delete(playlist: PlaylistEntity)

    @Delete
    abstract fun deleteBlocking(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists")
    abstract suspend fun deleteAll()

    @Query("DELETE FROM playlists WHERE deleted = 1")
    abstract fun deleteDeletedBlocking()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertBlocking(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET sortPosition = :position WHERE uuid = :uuid")
    abstract fun updateSortPositionBlocking(position: Int, uuid: String)

    @Query("UPDATE playlists SET syncStatus = :syncStatus WHERE uuid = :uuid")
    abstract fun updateSyncStatusBlocking(syncStatus: Int, uuid: String)

    @Query("UPDATE playlists SET syncStatus = :syncStatus")
    abstract suspend fun updateAllSyncStatus(syncStatus: Int)

    @Query("SELECT * FROM playlists WHERE UPPER(title) = UPPER(:title)")
    abstract fun searchByTitleBlocking(title: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE manual = 0 AND draft = 0 AND syncStatus = " + PlaylistEntity.SYNC_STATUS_NOT_SYNCED)
    abstract fun findNotSyncedBlocking(): List<PlaylistEntity>

    @Transaction
    open fun updateSortPositionsBlocking(playlists: List<PlaylistEntity>) {
        for (index in playlists.indices) {
            val playlist = playlists[index]
            val position = index + 1
            playlist.sortPosition = position
            playlist.syncStatus = PlaylistEntity.SYNC_STATUS_NOT_SYNCED
            updateSortPositionBlocking(position, playlist.uuid)
            updateSyncStatusBlocking(PlaylistEntity.SYNC_STATUS_NOT_SYNCED, playlist.uuid)
        }
    }
}
