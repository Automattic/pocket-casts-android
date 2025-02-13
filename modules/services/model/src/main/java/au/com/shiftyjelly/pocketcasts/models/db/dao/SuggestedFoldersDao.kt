package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolder

@Dao
abstract class SuggestedFoldersDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(folder: SuggestedFolder)

    @Query("SELECT * FROM suggested_folders WHERE uuid = :uuid")
    abstract suspend fun findByUuid(uuid: String): SuggestedFolder?

    @Query("SELECT * FROM suggested_folders WHERE folder_name = :folderName")
    abstract suspend fun findAllFolderPodcasts(folderName: String): List<SuggestedFolder>
}
