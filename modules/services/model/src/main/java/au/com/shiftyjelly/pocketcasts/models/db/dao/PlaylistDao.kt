package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import io.reactivex.Flowable
import io.reactivex.Maybe
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaylistDao {

    @Query("SELECT * FROM filters WHERE _id = :id")
    abstract fun findByIdBlocking(id: Long): Playlist?

    @Query("SELECT * FROM filters WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun findAllBlocking(): List<Playlist>

    @Query("SELECT * FROM filters WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract suspend fun findAll(): List<Playlist>

    @Query("SELECT * FROM filters WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun findAllFlow(): Flow<List<Playlist>>

    @Query("SELECT * FROM filters WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun findAllRxFlowable(): Flowable<List<Playlist>>

    @Query("SELECT * FROM filters WHERE uuid = :uuid LIMIT 1")
    abstract fun findByUuidBlocking(uuid: String): Playlist?

    @Query("SELECT * FROM filters WHERE uuid = :uuid LIMIT 1")
    abstract suspend fun findByUuid(uuid: String): Playlist?

    @Query("SELECT * FROM filters WHERE uuid = :uuid LIMIT 1")
    abstract fun findByUuidRxMaybe(uuid: String): Maybe<Playlist>

    @Query("SELECT * FROM filters WHERE uuid = :uuid LIMIT 1")
    abstract fun findByUuidRxFlowable(uuid: String): Flowable<Playlist>

    @Query("SELECT * FROM filters WHERE uuid = :uuid")
    abstract fun findByUuidAsListRxFlowable(uuid: String): Flowable<List<Playlist>>

    @Query("SELECT COUNT(*) FROM filters")
    abstract fun countBlocking(): Int

    @Update
    abstract fun updateBlocking(playlist: Playlist)

    @Update
    abstract fun updateAllBlocking(playlists: List<Playlist>)

    @Delete
    abstract fun deleteBlocking(playlist: Playlist)

    @Query("DELETE FROM filters")
    abstract suspend fun deleteAll()

    @Query("DELETE FROM filters WHERE deleted = 1")
    abstract fun deleteDeletedBlocking()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertBlocking(playlist: Playlist): Long

    @Query("UPDATE filters SET sortPosition = :position WHERE uuid = :uuid")
    abstract fun updateSortPositionBlocking(position: Int, uuid: String)

    @Query("UPDATE filters SET syncStatus = :syncStatus WHERE uuid = :uuid")
    abstract fun updateSyncStatusBlocking(syncStatus: Int, uuid: String)

    @Query("UPDATE filters SET syncStatus = :syncStatus")
    abstract fun updateAllSyncStatusBlocking(syncStatus: Int)

    @Query("SELECT * FROM filters WHERE UPPER(title) = UPPER(:title)")
    abstract fun searchByTitleBlocking(title: String): Playlist?

    @Query("SELECT * FROM filters WHERE manual = 0 AND draft = 0 AND syncStatus = " + Playlist.SYNC_STATUS_NOT_SYNCED)
    abstract fun findNotSyncedBlocking(): List<Playlist>

    @Transaction
    open fun updateSortPositionsBlocking(playlists: List<Playlist>) {
        for (index in playlists.indices) {
            val playlist = playlists[index]
            val position = index + 1
            playlist.sortPosition = position
            playlist.syncStatus = Playlist.SYNC_STATUS_NOT_SYNCED
            updateSortPositionBlocking(position, playlist.uuid)
            updateSyncStatusBlocking(Playlist.SYNC_STATUS_NOT_SYNCED, playlist.uuid)
        }
    }
}
