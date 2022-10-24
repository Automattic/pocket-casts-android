package au.com.shiftyjelly.pocketcasts.servers.bumpstats

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import timber.log.Timber
import java.util.Locale

@JsonClass(generateAdapter = true)
data class AnonymousBumpStatsRequest(
    @field:Json(name = "events") val events: List<Event> = emptyList(),
    @field:Json(name = "commonProps") val commonProps: CommonProps = CommonProps.get()
) {

    data class Event(
        val name: String,
        val eventTime: Long,
        val customEventProps: Map<String, Any> = emptyMap()
    ) {

        val uuid = "ANONYMOUS"
        val userType = "anon"

        companion object {
            object JsonKey {
                const val NAME = "_en"
                const val EVENT_TIME = "_ts"
                const val UUID = "_ui"
                const val USER_TYPE = "_ut"
            }
        }

        object Adapter {
            @ToJson
            fun toJson(event: Event): Map<String, Any> =
                buildMap {
                    put(JsonKey.NAME, event.name)
                    put(JsonKey.EVENT_TIME, event.eventTime)
                    put(JsonKey.UUID, event.uuid)
                    put(JsonKey.USER_TYPE, event.userType)

                    // include custom event props in root object
                    event.customEventProps.forEach { (k, v) ->
                        put(k, v)
                    }
                }

            @FromJson
            fun fromJson(event: Map<String, Any>): Event? {
                val eventProps = buildMap {
                    event.keys.forEach { key ->
                        when (key) {
                            JsonKey.NAME,
                            JsonKey.EVENT_TIME,
                            JsonKey.USER_TYPE,
                            JsonKey.UUID -> { /* ignore fields that are not dynamic */
                            }

                            else -> {
                                // include dynamic values in eventProps
                                event[key]?.let { value ->
                                    put(key, value)
                                }
                                Unit
                            }
                        }
                    }
                }

                val name = event[JsonKey.NAME]
                    as? String
                    ?: run {
                        Timber.e("Failed to parse ${JsonKey.NAME} for ${Event::class.qualifiedName}")
                        return null
                    }

                val eventTime = event[JsonKey.EVENT_TIME]
                    as? Long
                    ?: run {
                        Timber.e("Failed to parse ${JsonKey.EVENT_TIME} for ${Event::class.qualifiedName}")
                        return null
                    }

                return Event(
                    name = name,
                    eventTime = eventTime,
                    customEventProps = eventProps
                )
            }
        }
    }

    @JsonClass(generateAdapter = true)
    data class CommonProps(
        @field:Json(name = "_lg") val language: String,
        @field:Json(name = "_rt") val requestTime: Long,
        @field:Json(name = "_via_ua") val source: String
    ) {
        companion object {
            fun get() =
                CommonProps(
                    language = Locale.getDefault().toString(),
                    requestTime = System.currentTimeMillis(),
                    source = "Pocket Casts Android"
                )
        }
    }
}
