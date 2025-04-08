package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import au.com.shiftyjelly.pocketcasts.models.entity.Notifications

@Dao
abstract class NotificationsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(notifications: List<Notifications>): List<Long>
}
