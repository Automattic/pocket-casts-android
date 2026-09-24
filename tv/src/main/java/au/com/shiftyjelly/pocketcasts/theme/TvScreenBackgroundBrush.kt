package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Shared screen background so detail overlays fully occlude the content fading underneath them.
val TvScreenBackgroundBrush: Brush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF3D4045),
        Color(0xFF22252A),
    ),
)
