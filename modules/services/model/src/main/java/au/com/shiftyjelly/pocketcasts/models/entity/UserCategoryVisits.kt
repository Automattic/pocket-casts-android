package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_category_visits")
data class UserCategoryVisits(
    @PrimaryKey @ColumnInfo(name = "category_id") var categoryId: Int,
    @ColumnInfo(name = "total_visits") var totalVisits: Int = 0,
)
