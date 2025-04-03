package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_notifications")
data class UserNotifications(
    @PrimaryKey @ColumnInfo(name = "user_id") var userId: String,
    @ColumnInfo(name = "notification_id") var notificationId: Int,
    @ColumnInfo(name = "notifications_sent_this_week") var notificationsSentThisWeek: Int,
    @ColumnInfo(name = "last_notification_sent_at") var lastNotificationSentAt: Long,
    @ColumnInfo(name = "interacted_at") var interactedAt: Long? = null,
)
