package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.type.NotificationTriggerRule

class NotificationTriggerRuleConverter {

    @TypeConverter
    fun toNotificationTriggerRule(value: Int?): NotificationTriggerRule? {
        return if (value == null) null else NotificationTriggerRule.entries[value]
    }

    @TypeConverter
    fun toInt(value: NotificationTriggerRule?): Int? = value?.ordinal
}
