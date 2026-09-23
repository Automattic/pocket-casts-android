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

data class LossyList<T>(
    val values: List<T> = emptyList(),
    val droppedCount: Int = 0,
)

class LossyListAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (annotations.isNotEmpty() || Types.getRawType(type) != LossyList::class.java) return null

        val elementType = (type as? ParameterizedType)?.actualTypeArguments?.singleOrNull() ?: return null
        val listType = Types.newParameterizedType(List::class.java, elementType)
        return LossyListAdapter(moshi.adapter<Any>(elementType), moshi.adapter<List<Any>>(listType))
    }
}

private class LossyListAdapter<T>(
    private val elementAdapter: JsonAdapter<T>,
    private val listAdapter: JsonAdapter<List<T>>,
) : JsonAdapter<LossyList<T>>() {
    override fun fromJson(reader: JsonReader): LossyList<T> {
        if (reader.peek() == JsonReader.Token.NULL) {
            reader.skipValue()
            return LossyList()
        }
        if (reader.peek() != JsonReader.Token.BEGIN_ARRAY) {
            Timber.w("Dropping a list that was not published as an array")
            reader.skipValue()
            return LossyList(droppedCount = 1)
        }

        val values = mutableListOf<T>()
        var droppedCount = 0
        reader.beginArray()
        while (reader.hasNext()) {
            val element = reader.peekJson().use { peeked ->
                try {
                    elementAdapter.fromJson(peeked)
                } catch (e: JsonDataException) {
                    Timber.w(e, "Dropping an entry that could not be decoded")
                    null
                }
            }
            reader.skipValue()
            if (element == null) droppedCount++ else values.add(element)
        }
        reader.endArray()
        return LossyList(values, droppedCount)
    }

    override fun toJson(writer: JsonWriter, value: LossyList<T>?) {
        listAdapter.toJson(writer, value?.values)
    }
}
