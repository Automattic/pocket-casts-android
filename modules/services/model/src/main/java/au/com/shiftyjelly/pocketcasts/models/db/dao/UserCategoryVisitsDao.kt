package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import au.com.shiftyjelly.pocketcasts.models.entity.UserCategoryVisits

@Dao
abstract class UserCategoryVisitsDao {

    @Insert
    abstract suspend fun insert(categoryVisits: UserCategoryVisits)

    @Update
    abstract suspend fun update(categoryVisits: UserCategoryVisits)

    @Query("SELECT * from user_category_visits WHERE category_id = :id LIMIT 1")
    abstract suspend fun getVisitsById(id: Int): UserCategoryVisits?

    @Query("SELECT * from user_category_visits ORDER BY total_visits DESC")
    abstract suspend fun getCategoryVisitsOrdered(): List<UserCategoryVisits>

    suspend fun incrementVisits(id: Int) {
        val categoryVisits = getVisitsById(id)
        if (categoryVisits != null) {
            update(categoryVisits.copy(totalVisits = categoryVisits.totalVisits + 1))
        } else {
            insert(UserCategoryVisits(categoryId = id, totalVisits = 1))
        }
    }
}
