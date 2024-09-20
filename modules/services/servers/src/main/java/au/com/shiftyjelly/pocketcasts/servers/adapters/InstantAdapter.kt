package au.com.shiftyjelly.pocketcasts.servers.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.Instant
import java.util.Date

internal class InstantAdapter {
    @FromJson
    fun fromJson(date: Date): Instant = date.toInstant()

    @ToJson
    fun toJson(instant: Instant): Date = Date.from(instant)
}
