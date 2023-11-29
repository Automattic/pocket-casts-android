package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import io.sentry.Sentry
import timber.log.Timber
import java.time.Instant
import java.util.Date

class DateTypeConverter {

    @TypeConverter
    fun toDate(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun toLong(value: Date?): Long? {
        return value?.time
    }
}

typealias SafeDate = Date

// Type converter for dates that will not return null even if a null parameter is passed in.
class ShouldNotBeNullDateTypeConverter {

    @TypeConverter
    fun toDate(value: Long?): SafeDate {
        return if (value == null) {
            "ShouldNotBeNullDateTypeConverter::toDate called with null parameter. Returning epoch date.".let {
                Timber.w(it)
                Sentry.addBreadcrumb(it)
            }
            EPOCH
        } else {
            Date(value)
        }
    }

    @TypeConverter
    fun toLong(value: SafeDate?): Long = value?.time ?: 0L

    companion object {
        private val EPOCH = Date(Instant.EPOCH.toEpochMilli())
    }
}
