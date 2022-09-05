package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(folder: Folder)

    @Update
    abstract suspend fun update(folder: Folder)

    @Delete
    abstract suspend fun delete(folder: Folder)

    @Query("DELETE FROM folders WHERE uuid = :uuid")
    abstract suspend fun deleteByUuid(uuid: String)

    @Query("SELECT * FROM folders WHERE uuid = :uuid")
    abstract suspend fun findByUuid(uuid: String): Folder?

    @Query("SELECT * FROM folders WHERE uuid = :uuid")
    abstract fun findByUuidBlocking(uuid: String): Folder?

    @Query("SELECT * FROM folders WHERE uuid = :uuid")
    abstract fun findByUuidFlowable(uuid: String): Flowable<List<Folder>>

    @Query("SELECT * FROM folders WHERE uuid = :uuid")
    abstract fun findByUuidFlow(uuid: String): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE deleted = 0")
    abstract fun observeFolders(): Flowable<List<Folder>>

    @Query("SELECT * FROM folders WHERE deleted = 0")
    abstract fun findFoldersFlow(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE deleted = 0")
    abstract fun findFoldersSingle(): Single<List<Folder>>

    @Query("SELECT * FROM folders WHERE deleted = 0")
    abstract suspend fun findFolders(): List<Folder>

    @Query("SELECT * FROM folders WHERE sync_modified != 0")
    abstract fun findNotSynced(): List<Folder>

    @Query("UPDATE folders SET color = :color, sync_modified = :syncModified WHERE uuid = :uuid")
    abstract suspend fun updateFolderColor(uuid: String, color: Int, syncModified: Long)

    @Query("UPDATE folders SET name = :name, sync_modified = :syncModified WHERE uuid = :uuid")
    abstract suspend fun updateFolderName(uuid: String, name: String, syncModified: Long)

    @Query("UPDATE folders SET podcasts_sort_type = :podcastsSortType, sync_modified = :syncModified WHERE uuid = :uuid")
    abstract suspend fun updateFolderSortType(uuid: String, podcastsSortType: PodcastsSortType, syncModified: Long)

    @Query("UPDATE folders SET deleted = :deleted, sync_modified = :syncModified WHERE uuid = :uuid")
    abstract suspend fun updateDeleted(uuid: String, deleted: Boolean, syncModified: Long)

    @Query("UPDATE folders SET sort_position = :sortPosition, sync_modified = :syncModified WHERE uuid = :uuid")
    abstract suspend fun updateSortPosition(sortPosition: Int, uuid: String, syncModified: Long)

    @Query("UPDATE folders SET sync_modified = 0")
    abstract fun updateAllSynced()

    @Transaction
    open suspend fun updateSortPositions(folders: List<Folder>, syncModified: Long) {
        for (folder in folders) {
            updateSortPosition(sortPosition = folder.sortPosition, uuid = folder.uuid, syncModified = syncModified)
        }
    }

    @Query("SELECT COUNT(*) FROM folders WHERE deleted = 0")
    abstract fun count(): Int
}
