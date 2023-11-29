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

typealias ShouldNotBeNullDate = Date

// Type converter for dates that will not return null even if a null parameter is passed in.
class ShouldNotBeNullDateTypeConverter {

    @TypeConverter
    fun toDate(value: Long?): ShouldNotBeNullDate {
        return if (value == null) {
            "ShouldNotBeNullDateTypeConverter::toDate called with null parameter. Returning epoch date.".let {
                Timber.w(it)
                Sentry.addBreadcrumb(it)
            }
            Date(Instant.EPOCH.toEpochMilli())
        } else {
            Date(value)
        }
    }

    @TypeConverter
    fun toLong(value: ShouldNotBeNullDate?): Long = value?.time ?: 0L
}
