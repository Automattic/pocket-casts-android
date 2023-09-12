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
    abstract fun findById(id: Long): Playlist?

    @Query("SELECT * FROM filters WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun findAll(): List<Playlist>

    @Query("SELECT * FROM filters WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract suspend fun findAllSuspend(): List<Playlist>

    @Query("SELECT * FROM filters WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun findAllState(): Flow<List<Playlist>>

    @Query("SELECT * FROM filters WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun observeAll(): Flowable<List<Playlist>>

    @Query("SELECT * FROM filters WHERE uuid = :uuid LIMIT 1")
    abstract fun findByUuidSync(uuid: String): Playlist?

    @Query("SELECT * FROM filters WHERE uuid = :uuid LIMIT 1")
    abstract suspend fun findByUuid(uuid: String): Playlist?

    @Query("SELECT * FROM filters WHERE uuid = :uuid LIMIT 1")
    abstract fun findByUUIDRx(uuid: String): Maybe<Playlist>

    @Query("SELECT * FROM filters WHERE uuid = :uuid LIMIT 1")
    abstract fun observeByUUID(uuid: String): Flowable<Playlist>

    @Query("SELECT * FROM filters WHERE uuid = :uuid")
    abstract fun observeByUUIDAsList(uuid: String): Flowable<List<Playlist>>

    @Query("SELECT COUNT(*) FROM filters")
    abstract fun count(): Int

    @Query("SELECT COUNT(*) FROM filter_episodes")
    abstract fun countEpisodes(): Int

    @Update
    abstract fun update(playlist: Playlist)

    @Update
    abstract fun updateAll(playlists: List<Playlist>)

    @Delete
    abstract fun delete(playlist: Playlist)

    @Query("DELETE FROM filters")
    abstract suspend fun deleteAll()

    @Query("DELETE FROM filters WHERE deleted = 1")
    abstract fun deleteDeleted()

    @Query("DELETE FROM filter_episodes WHERE episodeUuid = :uuid")
    abstract fun deleteEpisodeByUuid(uuid: String)

    @Query("DELETE FROM filter_episodes WHERE playlistId = :playlistId")
    abstract fun deleteEpisodesByPlaylistId(playlistId: Long)

    @Query("DELETE FROM filter_episodes WHERE playlistId = :playlistId AND episodeUuid IN (:uuids)")
    abstract fun deleteEpisodesByPlaylistIdAndUuid(playlistId: Long, uuids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(playlist: Playlist): Long

    @Query("UPDATE filters SET sortPosition = :position WHERE uuid = :uuid")
    abstract fun updateSortPosition(position: Int, uuid: String)

    @Query("UPDATE filters SET syncStatus = :syncStatus WHERE uuid = :uuid")
    abstract fun updateSyncStatus(syncStatus: Int, uuid: String)

    @Query("UPDATE filters SET syncStatus = :syncStatus")
    abstract fun updateAllSyncStatus(syncStatus: Int)

    @Query("UPDATE filter_episodes SET position = :position WHERE _id = :id")
    abstract fun updateEpisodePosition(position: Int, id: Long)

    @Query("SELECT * FROM filters WHERE UPPER(title) = UPPER(:title)")
    abstract fun searchByTitle(title: String): Playlist?

    @Query("SELECT * FROM filters WHERE manual = 0 AND draft = 0 AND syncStatus = " + Playlist.SYNC_STATUS_NOT_SYNCED)
    abstract fun findNotSynced(): List<Playlist>

    @Transaction
    open fun updateSortPositions(playlists: List<Playlist>) {
        for (index in playlists.indices) {
            val playlist = playlists[index]
            val position = index + 1
            playlist.sortPosition = position
            playlist.syncStatus = Playlist.SYNC_STATUS_NOT_SYNCED
            updateSortPosition(position, playlist.uuid)
            updateSyncStatus(Playlist.SYNC_STATUS_NOT_SYNCED, playlist.uuid)
        }
    }
}
