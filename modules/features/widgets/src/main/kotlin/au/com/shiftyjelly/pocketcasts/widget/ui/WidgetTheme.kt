package au.com.shiftyjelly.pocketcasts.widget.ui

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceComposable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders

@Composable
internal fun WidgetTheme(
    useDynamicColors: Boolean,
    content:
    @GlanceComposable @Composable
    () -> Unit,
) {
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && useDynamicColors) {
        GlanceTheme.colors
    } else {
        DefaultColors
    }
    GlanceTheme(colors, content)
}

private val UndefinedColor = Color.Magenta

private val DefaultColors = ColorProviders(
    light = ColorScheme(
        primary = Color(0xFFF8FAF6),
        onPrimary = Color(0xFFF43E37),
        primaryContainer = Color(0xFFF43E37),
        // Used for old widget icon colors in non-dynamic theme.
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = UndefinedColor,
        // Used as a trick to have different small player placeholder colors.
        secondary = Color(0xFFF43E37),
        onSecondary = Color(0xFFF8FAF6),
        secondaryContainer = UndefinedColor,
        onSecondaryContainer = UndefinedColor,
        tertiary = UndefinedColor,
        onTertiary = UndefinedColor,
        tertiaryContainer = UndefinedColor,
        onTertiaryContainer = UndefinedColor,
        background = UndefinedColor,
        onBackground = UndefinedColor,
        // Used as a trick to have different small player button colors for small widget with non dynamic themes in dark mode.
        surface = Color(0xFFF8FAF6),
        onSurface = Color(0xFFF43E37),
        surfaceVariant = UndefinedColor,
        onSurfaceVariant = UndefinedColor,
        surfaceTint = UndefinedColor,
        inverseSurface = UndefinedColor,
        inverseOnSurface = UndefinedColor,
        error = UndefinedColor,
        onError = UndefinedColor,
        errorContainer = UndefinedColor,
        onErrorContainer = UndefinedColor,
        outline = UndefinedColor,
        outlineVariant = UndefinedColor,
        scrim = UndefinedColor,
    ),
    dark = ColorScheme(
        primary = Color(0xFFD9201C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF292B2E),
        // Used for old widget icon colors in non-dynamic theme.
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = UndefinedColor,
        // Used as a trick to have different small player placeholder colors.
        secondary = Color(0xFFD9201C),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = UndefinedColor,
        onSecondaryContainer = UndefinedColor,
        tertiary = UndefinedColor,
        onTertiary = UndefinedColor,
        tertiaryContainer = UndefinedColor,
        onTertiaryContainer = UndefinedColor,
        background = UndefinedColor,
        onBackground = UndefinedColor,
        // Used as a trick to have different small player button colors for small widget with non dynamic themes.
        surface = Color(0xFFF8FAF6),
        onSurface = Color(0xFFD9201C),
        surfaceVariant = UndefinedColor,
        onSurfaceVariant = UndefinedColor,
        surfaceTint = UndefinedColor,
        inverseSurface = UndefinedColor,
        inverseOnSurface = UndefinedColor,
        error = UndefinedColor,
        onError = UndefinedColor,
        errorContainer = UndefinedColor,
        onErrorContainer = UndefinedColor,
        outline = UndefinedColor,
        outlineVariant = UndefinedColor,
        scrim = UndefinedColor,
    ),
)
