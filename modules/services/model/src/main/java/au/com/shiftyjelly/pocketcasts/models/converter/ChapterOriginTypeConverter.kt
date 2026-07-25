package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.to.ChapterOrigin

class ChapterOriginTypeConverter {
    @TypeConverter
    fun fromInt(value: Int?): ChapterOrigin {
        return value?.let { ChapterOrigin.fromId(it) } ?: ChapterOrigin.Unknown
    }

    @TypeConverter
    fun toInt(value: ChapterOrigin?): Int {
        return value?.id ?: ChapterOrigin.Unknown.id
    }
}
