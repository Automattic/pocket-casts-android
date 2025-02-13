package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolder
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SuggestedFoldersDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(folder: SuggestedFolder)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAll(folders: List<SuggestedFolder>)

    @Query("SELECT * FROM suggested_folders WHERE folder_name = :folderName")
    abstract suspend fun findAllFolderPodcasts(folderName: String): List<SuggestedFolder>

    @Query("SELECT * FROM suggested_folders")
    abstract fun findAll(): Flow<List<SuggestedFolder>>
}
