package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import au.com.shiftyjelly.pocketcasts.models.entity.SearchHistoryItem

@Dao
abstract class SearchHistoryDao {
    @Transaction
    @Query("SELECT * FROM search_history ORDER BY modified DESC")
    abstract fun findAll(): List<SearchHistoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(searchHistoryItem: SearchHistoryItem)

    @Delete
    abstract fun delete(result: SearchHistoryItem)

    @Query("DELETE FROM search_history")
    abstract suspend fun deleteAll()
}
