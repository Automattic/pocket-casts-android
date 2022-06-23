package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.to.BundlePaidType
import java.util.Locale

class BundlePaidTypeConverter {
    @TypeConverter
    fun toPaidType(string: String?): BundlePaidType? {
        return if (string == null) null else BundlePaidType.valueOf(string.uppercase(Locale.ROOT))
    }

    @TypeConverter
    fun toString(value: BundlePaidType?): String? {
        return value?.name?.uppercase(Locale.ROOT)
    }
}
