package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.type.NotificationCategory

class NotificationCategoryConverter {

    @TypeConverter
    fun toNotificationCategory(value: Int?): NotificationCategory? {
        return if (value == null) null else NotificationCategory.entries[value]
    }

    @TypeConverter
    fun toInt(value: NotificationCategory?): Int? = value?.ordinal
}
