package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus

class SyncStatusConverter {

    @TypeConverter
    fun toSyncStatus(value: Int?): SyncStatus? {
        return if (value == null) null else SyncStatus.values()[value]
    }

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus?): Int? {
        return value?.ordinal
    }
}
