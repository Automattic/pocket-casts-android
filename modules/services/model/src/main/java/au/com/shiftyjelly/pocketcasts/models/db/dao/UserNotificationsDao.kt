package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import au.com.shiftyjelly.pocketcasts.models.entity.UserNotifications

@Dao
abstract class UserNotificationsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(userNotifications: List<UserNotifications>)

    @Query("UPDATE user_notifications SET interacted_at = :interactedAt WHERE notification_id = :notificationId")
    abstract suspend fun updateInteractedAt(notificationId: Int, interactedAt: Long)

    @Query("SELECT * FROM user_notifications WHERE notification_id = :notificationId LIMIT 1")
    abstract suspend fun getUserNotification(notificationId: Int): UserNotifications?

    @Update
    abstract suspend fun update(userNotifications: UserNotifications)

    @Query("DELETE FROM user_notifications")
    abstract suspend fun deleteAll()
}
