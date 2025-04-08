package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import au.com.shiftyjelly.pocketcasts.models.entity.UserNotifications

@Dao
abstract class UserNotificationsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(userNotifications: List<UserNotifications>)
}
