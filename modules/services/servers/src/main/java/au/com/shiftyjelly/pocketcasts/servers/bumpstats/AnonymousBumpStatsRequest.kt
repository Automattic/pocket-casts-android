package au.com.shiftyjelly.pocketcasts.servers.bumpstats

import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Locale

@JsonClass(generateAdapter = true)
data class AnonymousBumpStatsRequest(
    @field:Json(name = "events") val events: List<AnonymousBumpStat> = emptyList(),
    @field:Json(name = "commonProps") val commonProps: CommonProps = CommonProps.get()
) {
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
