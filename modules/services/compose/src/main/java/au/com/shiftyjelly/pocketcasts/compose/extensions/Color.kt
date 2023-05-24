package au.com.shiftyjelly.pocketcasts.compose.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

fun Color.darker(factor: Float = 1f) =
    Color(ColorUtils.blendARGB(this.toArgb(), Color.Black.toArgb(), factor))
