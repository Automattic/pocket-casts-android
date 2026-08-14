package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.tv.material3.MaterialTheme

// Shared screen background so detail overlays fully occlude the content fading underneath them.
val TvScreenBackgroundBrush: Brush
    @Composable
    @ReadOnlyComposable
    get() = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.tvColors.backgroundBase,
            MaterialTheme.tvColors.backgroundSunken,
        ),
    )
