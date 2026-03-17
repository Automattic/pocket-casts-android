package au.com.shiftyjelly.pocketcasts.servers.analytics

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.time.Instant

@JsonClass(generateAdapter = true)
data class InputEvent(
    val name: String,
    val timestamp: Instant,
    val properties: EventProperties,
    val platform: String,
)

data class EventProperties(
    val value: Map<String, Any>,
)

internal class EventPropertiesJsonAdapter : JsonAdapter<EventProperties>() {
    override fun fromJson(reader: JsonReader): EventProperties {
        error("Deserialization of EventProperties is not supported")
    }

    override fun toJson(writer: JsonWriter, properties: EventProperties?) {
        requireNotNull(properties) {
            "properties was null! Wrap in .nullSafe() to write nullable values."
        }
        writer.beginObject()
        for ((key, value) in properties.value) {
            writer.name(key)
            when (value) {
                is String -> writer.value(value)
                is Long -> writer.value(value)
                is Double -> writer.value(value)
                is Number -> writer.value(value)
                is Boolean -> writer.value(value)
                else -> writer.value(value.toString())
            }
        }
        writer.endObject()
    }
}
