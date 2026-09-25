package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.entity.AlternateEnclosureSource
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@ProvidedTypeConverter
class AlternateEnclosureSourcesConverter(
    moshi: Moshi,
) {
    private val adapter = moshi.adapter<List<AlternateEnclosureSource>>(
        Types.newParameterizedType(List::class.java, AlternateEnclosureSource::class.java),
    )

    @TypeConverter
    fun toSources(value: String?): List<AlternateEnclosureSource> = value?.let { adapter.fromJson(it) }.orEmpty()

    @TypeConverter
    fun toJsonString(sources: List<AlternateEnclosureSource>): String = adapter.toJson(sources)
}
