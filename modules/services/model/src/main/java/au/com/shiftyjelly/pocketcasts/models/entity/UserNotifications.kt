package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_notifications")
data class UserNotifications(
    @PrimaryKey @ColumnInfo(name = "user_id") var userId: String,
    @ColumnInfo(name = "notification_id") var notificationId: Int,
    @ColumnInfo(name = "sent_this_week") var sentThisWeek: Int,
    @ColumnInfo(name = "last_sent_at") var lastSentAt: Long,
    @ColumnInfo(name = "interacted_at") var interactedAt: Long? = null,
)
