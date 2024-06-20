package au.com.shiftyjelly.pocketcasts.clip

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils

internal data class ClipColors(
    val baseColor: Color,
) {
    val backgroundColor = ColorUtils.changeHsvValue(baseColor, factor = 0.4f)
    val backgroundTextColor = if (backgroundColor.luminance() < 0.5f) Color.White else Color.Black

    val buttonColor = if (backgroundColor.luminance() < 0.25) {
        ColorUtils.changeHsvValue(baseColor, 1.25f)
    } else {
        baseColor
    }
    val buttonTextColor = if (buttonColor.luminance() < 0.5f) Color.White else Color.Black

    val cardTop = ColorUtils.changeHsvValue(baseColor, 1.25f)
    val cardBottom = ColorUtils.changeHsvValue(baseColor, 0.75f)
    val cardTextColor = if (baseColor.luminance() < 0.5f) Color.White else Color.Black
}
