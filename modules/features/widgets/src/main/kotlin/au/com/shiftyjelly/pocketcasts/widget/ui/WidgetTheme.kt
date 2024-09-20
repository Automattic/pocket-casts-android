package au.com.shiftyjelly.pocketcasts.widget.ui

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceComposable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider

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
    GlanceTheme(colors) {
        val glanceColors = GlanceTheme.colors
        val widgetColors = WidgetTheme(
            background = glanceColors.secondaryContainer,
            text = glanceColors.onSecondaryContainer,
            buttonBackground = glanceColors.onSecondary,
            icon = glanceColors.secondary,
            logoBackground = if (useDynamicColors) glanceColors.secondary else glanceColors.tertiary,
            logoLines = if (useDynamicColors) glanceColors.onSecondary else glanceColors.onTertiary,
        )
        CompositionLocalProvider(LocalWidgetTheme provides widgetColors, content)
    }
}

internal val LocalWidgetTheme = staticCompositionLocalOf<WidgetTheme> { error("No default widget colors") }

internal data class WidgetTheme(
    val background: ColorProvider,
    val text: ColorProvider,
    val buttonBackground: ColorProvider,
    val icon: ColorProvider,
    val logoBackground: ColorProvider,
    val logoLines: ColorProvider,
)

private val UndefinedColor = Color.Magenta

private val DefaultColors = ColorProviders(
    light = ColorScheme(
        primary = UndefinedColor,
        onPrimary = UndefinedColor,
        primaryContainer = UndefinedColor,
        onPrimaryContainer = UndefinedColor,
        inversePrimary = UndefinedColor,
        secondary = Color(0xFF292B2E),
        onSecondary = Color(0xFFE0E6EA),
        secondaryContainer = Color(0xFFFAFAF9),
        onSecondaryContainer = Color(0xFF292B2E),
        // Used as a trick for different Pocket Casts logo colors
        tertiary = Color(0xFFF43E37),
        onTertiary = Color(0xFFFAFAF9),
        tertiaryContainer = UndefinedColor,
        onTertiaryContainer = UndefinedColor,
        background = UndefinedColor,
        onBackground = UndefinedColor,
        surface = UndefinedColor,
        onSurface = UndefinedColor,
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
        primary = UndefinedColor,
        onPrimary = UndefinedColor,
        primaryContainer = UndefinedColor,
        onPrimaryContainer = UndefinedColor,
        inversePrimary = UndefinedColor,
        secondary = Color(0xFFFFFFFF),
        onSecondary = Color(0xFF333438),
        secondaryContainer = Color(0xFF292B2E),
        onSecondaryContainer = Color(0xFFFFFFFF),
        // Used as a trick for different Pocket Casts logo colors
        tertiary = Color(0xFFD9201C),
        onTertiary = Color(0xFFFAFAF9),
        tertiaryContainer = UndefinedColor,
        onTertiaryContainer = UndefinedColor,
        background = UndefinedColor,
        onBackground = UndefinedColor,
        surface = UndefinedColor,
        onSurface = UndefinedColor,
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
