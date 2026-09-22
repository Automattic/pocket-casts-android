package au.com.shiftyjelly.pocketcasts.servers.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import timber.log.Timber

data class LossyList<T>(val values: List<T> = emptyList())

class LossyListAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (annotations.isNotEmpty() || Types.getRawType(type) != LossyList::class.java) return null

        val elementType = (type as? ParameterizedType)?.actualTypeArguments?.singleOrNull() ?: return null
        return LossyListAdapter(moshi.adapter<Any>(elementType))
    }
}

private class LossyListAdapter<T>(
    private val elementAdapter: JsonAdapter<T>,
) : JsonAdapter<LossyList<T>>() {
    override fun fromJson(reader: JsonReader): LossyList<T> {
        if (reader.peek() != JsonReader.Token.BEGIN_ARRAY) {
            reader.skipValue()
            return LossyList()
        }

        val values = mutableListOf<T>()
        reader.beginArray()
        while (reader.hasNext()) {
            val value = reader.readJsonValue()
            try {
                elementAdapter.fromJsonValue(value)?.let(values::add)
            } catch (e: JsonDataException) {
                Timber.w(e, "Dropping an entry that could not be decoded")
            }
        }
        reader.endArray()
        return LossyList(values)
    }

    override fun toJson(writer: JsonWriter, value: LossyList<T>?) {
        writer.beginArray()
        value?.values?.forEach { element -> elementAdapter.toJson(writer, element) }
        writer.endArray()
    }
}
