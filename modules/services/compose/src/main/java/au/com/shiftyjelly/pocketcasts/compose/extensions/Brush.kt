package au.com.shiftyjelly.pocketcasts.compose.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun rainbowBrush(
    start: Offset = Offset.Zero,
    end: Offset = Offset.Infinite,
) = Brush.linearGradient(
    0.00f to Color(red = 0.25f, green = 0.11f, blue = 0.92f),
    0.24f to Color(red = 0.68f, green = 0.89f, blue = 0.86f),
    0.50f to Color(red = 0.87f, green = 0.91f, blue = 0.53f),
    0.74f to Color(red = 0.91f, green = 0.35f, blue = 0.26f),
    1.00f to Color(red = 0.1f, green = 0.1f, blue = 0.1f),
    start = start,
    end = end,
)

