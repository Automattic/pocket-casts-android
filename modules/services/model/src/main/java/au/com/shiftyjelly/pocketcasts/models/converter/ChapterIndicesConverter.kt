package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.entity.ChapterIndices

class ChapterIndicesConverter {
    @TypeConverter
    fun fromString(value: String?): ChapterIndices {
        val list = value?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        return ChapterIndices(list)
    }

    @TypeConverter
    fun toString(indices: ChapterIndices) = indices.joinToString(",")
}
