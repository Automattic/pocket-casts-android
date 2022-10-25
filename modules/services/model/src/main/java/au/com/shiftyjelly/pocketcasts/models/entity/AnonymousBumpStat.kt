package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.TypeConverter
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import timber.log.Timber

@Entity(tableName = "bump_stats", primaryKeys = ["name", "event_time"])
data class AnonymousBumpStat(
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "event_time") var eventTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "custom_event_props") var customEventProps: Map<String, Any> = emptyMap()
) {
    companion object {
        object JsonKey {
            const val NAME = "_en"
            const val EVENT_TIME = "_ts"
            const val UUID = "_ui"
            const val USER_TYPE = "_ut"
        }

        const val userType = "anon"
        const val uuid = "ANONYMOUS"
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

        // Prefix that is applied to custom event prop keys in order to avoid possible clashes with
        // the other json keys like name, event_time, uuid, user_type.
        private const val CUSTOM_EVENT_PROP_PREFIX = "customEventProp_"

        @ToJson
        fun toJson(bumpStat: AnonymousBumpStat): Map<String, Any> =
            buildMap {
                put(JsonKey.NAME, bumpStat.name)
                put(JsonKey.EVENT_TIME, bumpStat.eventTime)
                put(JsonKey.UUID, uuid)
                put(JsonKey.USER_TYPE, userType)

                // include custom event props in root object
                bumpStat.customEventProps.forEach { (k, v) ->
                    put(CUSTOM_EVENT_PROP_PREFIX + k, v)
                }
            }

        @FromJson
        fun fromJson(bumpStatMap: Map<String, Any>): AnonymousBumpStat? {
            val eventProps = buildMap {
                bumpStatMap.keys.forEach { key ->
                    when (key) {
                        JsonKey.NAME,
                        JsonKey.EVENT_TIME,
                        JsonKey.USER_TYPE,
                        JsonKey.UUID -> { /* ignore fields that are not dynamic */ }

                        else -> {
                            // include dynamic values in eventProps
                            bumpStatMap[CUSTOM_EVENT_PROP_PREFIX + key]?.let { value ->
                                put(key, value)
                            }
                            Unit
                        }
                    }
                }
            }

            val name = bumpStatMap[JsonKey.NAME]
                as? String
                ?: run {
                    Timber.e("Failed to parse ${JsonKey.NAME} for ${AnonymousBumpStat::class.qualifiedName}")
                    return null
                }

            val eventTime = bumpStatMap[JsonKey.EVENT_TIME]
                as? Long
                ?: run {
                    Timber.e("Failed to parse ${JsonKey.EVENT_TIME} for ${AnonymousBumpStat::class.qualifiedName}")
                    return null
                }

            return AnonymousBumpStat(
                name = name,
                eventTime = eventTime,
                customEventProps = eventProps
            )
        }
    }
}
