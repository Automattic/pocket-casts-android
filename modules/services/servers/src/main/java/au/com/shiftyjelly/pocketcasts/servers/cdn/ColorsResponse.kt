package au.com.shiftyjelly.pocketcasts.servers.cdn

import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ColorsResponse(
    @Json(name = "colors") val colors: Colors?,
) {

    fun toArtworkColors(): ArtworkColors {
        return ArtworkColors(
            background = parseColor(colors?.background),
            tintForLightBg = parseColor(colors?.tintForLightBg),
            tintForDarkBg = parseColor(colors?.tintForDarkBg),
            timeDownloadedMs = System.currentTimeMillis(),
        )
    }

    private fun parseColor(colorString: String?): Int {
        return if (colorString.isNullOrEmpty()) Color.BLACK else colorString.toColorInt()
    }
}

@JsonClass(generateAdapter = true)
data class Colors(
    @Json(name = "background") val background: String,
    @Json(name = "tintForDarkBg") val tintForDarkBg: String,
    @Json(name = "tintForLightBg") val tintForLightBg: String,
)
