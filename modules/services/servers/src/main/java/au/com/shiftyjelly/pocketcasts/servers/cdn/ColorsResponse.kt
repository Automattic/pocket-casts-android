package au.com.shiftyjelly.pocketcasts.servers.cdn

import android.graphics.Color
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ColorsResponse(
    @field:Json(name = "colors") val colors: Colors?
) {

    fun toArtworkColors(): ArtworkColors {
        return ArtworkColors(
            background = parseColor(colors?.background),
            tintForLightBg = resetDefaultTint(parseColor(colors?.tintForLightBg), isTintLightColor = true),
            tintForDarkBg = resetDefaultTint(parseColor(colors?.tintForDarkBg), isTintLightColor = false),
            timeDownloadedMs = System.currentTimeMillis()
        )
    }

    private fun parseColor(colorString: String?): Int {
        return if (colorString.isNullOrEmpty()) 0 else Color.parseColor(colorString)
    }

    private fun resetDefaultTint(color: Int, isTintLightColor: Boolean): Int {
        return if (isTintLightColor) {
            if (color == DEFAULT_SERVER_LIGHT_TINT_COLOR) DEFAULT_LIGHT_TINT_COLOR else color
        } else {
            if (color == DEFAULT_SERVER_DARK_TINT_COLOR) DEFAULT_DARK_TINT_COLOR else color
        }
    }
}

@JsonClass(generateAdapter = true)
data class Colors(
    @field:Json(name = "background") val background: String,
    @field:Json(name = "tintForDarkBg") val tintForDarkBg: String,
    @field:Json(name = "tintForLightBg") val tintForLightBg: String
)

const val DEFAULT_LIGHT_TINT_COLOR = 0xFF1E1F1E.toInt()
const val DEFAULT_DARK_TINT_COLOR = 0xFFFFFFFF.toInt()

const val DEFAULT_SERVER_LIGHT_TINT_COLOR = 0xFFF44336.toInt()
const val DEFAULT_SERVER_DARK_TINT_COLOR = 0xFFC62828.toInt()
