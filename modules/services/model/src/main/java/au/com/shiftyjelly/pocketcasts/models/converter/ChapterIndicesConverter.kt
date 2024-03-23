package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.entity.ChapterIndices

class ChapterIndicesConverter {
    @TypeConverter
    fun fromString(value: String?) = ChapterIndices.fromString(value)

    @TypeConverter
    fun toString(indices: ChapterIndices) = ChapterIndices.toString(indices)
}
