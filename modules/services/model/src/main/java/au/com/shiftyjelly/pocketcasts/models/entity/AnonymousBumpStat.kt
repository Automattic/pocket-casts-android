package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.TypeConverter
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import timber.log.Timber

@Entity(tableName = "bump_stats", primaryKeys = ["name", "event_time", "custom_event_props"])
data class AnonymousBumpStat(
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "event_time") var eventTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "custom_event_props") var customEventProps: Map<String, Any> = emptyMap()
) {

    init {
        val conflictingKeys = customEventProps.keys.filter { rootJsonKeys.contains(it) }
        if (conflictingKeys.isNotEmpty()) {
            // There should never be conflicting keys because when we convert this object to json, the
            // rootJsonKeys and the customEventProp keys are all included at the root level. Fail
            // quickly if we have created conflicting keys since this is a developer error.
            throw IllegalStateException("customEventProps contained a key that conflicted with a root key: $conflictingKeys")
        }
    }

    fun withBumpName(): AnonymousBumpStat {
        val bumpName = "pcandroid_${name}_bump"
        return copy(name = bumpName)
    }

    companion object {

        private enum class JsonKey(val value: String) {
            NAME("_en"),
            EVENT_TIME("_ts"),
            UUID("_ui"),
            USER_TYPE("_ut")
        }

        private val rootJsonKeys = JsonKey.values().map { it.value }

        const val userTypeValue = "anon"
        const val uuidValue = "ANONYMOUS"
    }

    class CustomEventPropsTypeConverter {

        private val moshi = Moshi.Builder()
            .build()
            .adapter<Map<String, Any>>(
                Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
                )
            )

        @TypeConverter
        fun toCustomEventProps(value: String?): Map<String, Any>? =
            value?.let {
                moshi.fromJson(it)
            }

        @TypeConverter
        fun toJsonString(value: Map<String, Any>?): String? = moshi.toJson(value)
    }

    object Adapter {
        @ToJson
        fun toJson(bumpStat: AnonymousBumpStat): Map<String, Any> =
            buildMap {
                put(JsonKey.NAME.value, bumpStat.name)
                put(JsonKey.EVENT_TIME.value, bumpStat.eventTime)
                put(JsonKey.UUID.value, uuidValue)
                put(JsonKey.USER_TYPE.value, userTypeValue)

                // include custom event props in root object
                bumpStat.customEventProps.forEach { (k, v) ->
                    put(k, v)
                }
            }

        @FromJson
        fun fromJson(bumpStatMap: Map<String, Any>): AnonymousBumpStat? {

            val eventProps = bumpStatMap.filterKeys { !rootJsonKeys.contains(it) }

            val name = bumpStatMap[JsonKey.NAME.value]
                as? String
                ?: run {
                    Timber.e("Failed to parse ${JsonKey.NAME.value} for ${AnonymousBumpStat::class.qualifiedName}")
                    return null
                }

            val eventTime = bumpStatMap[JsonKey.EVENT_TIME.value]
                as? Long
                ?: run {
                    Timber.e("Failed to parse ${JsonKey.EVENT_TIME.value} for ${AnonymousBumpStat::class.qualifiedName}")
                    return null
                }

            // Ignoring JsonKey.UUID and JsonKey.USER_TYPE because those are hardcoded values

            return AnonymousBumpStat(
                name = name,
                eventTime = eventTime,
                customEventProps = eventProps
            )
        }
    }
}
