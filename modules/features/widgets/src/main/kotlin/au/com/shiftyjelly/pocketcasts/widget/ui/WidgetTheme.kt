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
        primary = Color(0xFF03A9F4),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFBFCFC),
        onPrimaryContainer = Color(0xFF303233),
        inversePrimary = UndefinedColor,
        secondary = Color(0xFF5DA9CC),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = UndefinedColor,
        onSecondaryContainer = UndefinedColor,
        tertiary = UndefinedColor,
        onTertiary = UndefinedColor,
        tertiaryContainer = UndefinedColor,
        onTertiaryContainer = UndefinedColor,
        background = Color(0xFFFBFCFC),
        onBackground = Color(0xFF303233),
        surface = Color(0xFFFBFCFC),
        onSurface = Color(0xFF303233),
        surfaceVariant = UndefinedColor,
        onSurfaceVariant = UndefinedColor,
        surfaceTint = UndefinedColor,
        inverseSurface = UndefinedColor,
        inverseOnSurface = UndefinedColor,
        error = Color(0xFFF43E37),
        onError = Color(0xFFFFFFFF),
        errorContainer = UndefinedColor,
        onErrorContainer = UndefinedColor,
        outline = UndefinedColor,
        outlineVariant = UndefinedColor,
        scrim = UndefinedColor,
    ),
    dark = ColorScheme(
        primary = Color(0xFF80C6E6),
        onPrimary = Color(0xFF01354C),
        primaryContainer = Color(0xFF303233),
        onPrimaryContainer = Color(0xFFE2E4E6),
        inversePrimary = UndefinedColor,
        secondary = Color(0xFFB8D7E6),
        onSecondary = Color(0xFF01354C),
        secondaryContainer = UndefinedColor,
        onSecondaryContainer = UndefinedColor,
        tertiary = UndefinedColor,
        onTertiary = UndefinedColor,
        tertiaryContainer = UndefinedColor,
        onTertiaryContainer = UndefinedColor,
        background = Color(0xFF303233),
        onBackground = Color(0xFFE2E4E6),
        surface = Color(0xFF303233),
        onSurface = Color(0xFFE2E4E6),
        surfaceVariant = UndefinedColor,
        onSurfaceVariant = UndefinedColor,
        surfaceTint = UndefinedColor,
        inverseSurface = UndefinedColor,
        inverseOnSurface = UndefinedColor,
        error = Color(0xFFE69996),
        onError = Color(0xFF4C1312),
        errorContainer = UndefinedColor,
        onErrorContainer = UndefinedColor,
        outline = UndefinedColor,
        outlineVariant = UndefinedColor,
        scrim = UndefinedColor,
    ),
)
