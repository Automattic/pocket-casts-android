package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import io.reactivex.Flowable
import io.reactivex.Maybe
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SmartPlaylistDao {

    @Query("SELECT * FROM smart_playlists WHERE _id = :id")
    abstract fun findByIdBlocking(id: Long): SmartPlaylist?

    @Query("SELECT * FROM smart_playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun findAllBlocking(): List<SmartPlaylist>

    @Query("SELECT * FROM smart_playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract suspend fun findAll(): List<SmartPlaylist>

    @Query("SELECT * FROM smart_playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun findAllFlow(): Flow<List<SmartPlaylist>>

    @Query("SELECT * FROM smart_playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun findAllRxFlowable(): Flowable<List<SmartPlaylist>>

    @Query("SELECT * FROM smart_playlists WHERE uuid = :uuid LIMIT 1")
    abstract fun findByUuidBlocking(uuid: String): SmartPlaylist?

    @Query("SELECT * FROM smart_playlists WHERE uuid = :uuid LIMIT 1")
    abstract suspend fun findByUuid(uuid: String): SmartPlaylist?

    @Query("SELECT * FROM smart_playlists WHERE uuid = :uuid LIMIT 1")
    abstract fun findByUuidRxMaybe(uuid: String): Maybe<SmartPlaylist>

    @Query("SELECT * FROM smart_playlists WHERE uuid = :uuid LIMIT 1")
    abstract fun findByUuidRxFlowable(uuid: String): Flowable<SmartPlaylist>

    @Query("SELECT * FROM smart_playlists WHERE uuid = :uuid")
    abstract fun findByUuidAsListRxFlowable(uuid: String): Flowable<List<SmartPlaylist>>

    @Query("SELECT COUNT(*) FROM smart_playlists")
    abstract suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM smart_playlists")
    abstract fun countBlocking(): Int

    @Update
    abstract suspend fun update(smartPlaylist: SmartPlaylist)

    @Update
    abstract fun updateBlocking(smartPlaylist: SmartPlaylist)

    @Update
    abstract fun updateAllBlocking(smartPlaylists: List<SmartPlaylist>)

    @Delete
    abstract suspend fun delete(smartPlaylist: SmartPlaylist)

    @Delete
    abstract fun deleteBlocking(smartPlaylist: SmartPlaylist)

    @Query("DELETE FROM smart_playlists")
    abstract suspend fun deleteAll()

    @Query("DELETE FROM smart_playlists WHERE deleted = 1")
    abstract fun deleteDeletedBlocking()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(smartPlaylist: SmartPlaylist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertBlocking(smartPlaylist: SmartPlaylist): Long

    @Query("UPDATE smart_playlists SET sortPosition = :position WHERE uuid = :uuid")
    abstract fun updateSortPositionBlocking(position: Int, uuid: String)

    @Query("UPDATE smart_playlists SET syncStatus = :syncStatus WHERE uuid = :uuid")
    abstract fun updateSyncStatusBlocking(syncStatus: Int, uuid: String)

    @Query("UPDATE smart_playlists SET syncStatus = :syncStatus")
    abstract suspend fun updateAllSyncStatus(syncStatus: Int)

    @Query("SELECT * FROM smart_playlists WHERE UPPER(title) = UPPER(:title)")
    abstract fun searchByTitleBlocking(title: String): SmartPlaylist?

    @Query("SELECT * FROM smart_playlists WHERE manual = 0 AND draft = 0 AND syncStatus = " + SmartPlaylist.SYNC_STATUS_NOT_SYNCED)
    abstract fun findNotSyncedBlocking(): List<SmartPlaylist>

    @Transaction
    open fun updateSortPositionsBlocking(smartPlaylists: List<SmartPlaylist>) {
        for (index in smartPlaylists.indices) {
            val playlist = smartPlaylists[index]
            val position = index + 1
            playlist.sortPosition = position
            playlist.syncStatus = SmartPlaylist.SYNC_STATUS_NOT_SYNCED
            updateSortPositionBlocking(position, playlist.uuid)
            updateSyncStatusBlocking(SmartPlaylist.SYNC_STATUS_NOT_SYNCED, playlist.uuid)
        }
    }
}
