package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

val LocalTvColorScheme = staticCompositionLocalOf { TvColorScheme() }
val LocalTvTypography = staticCompositionLocalOf { TvTypography() }

val MaterialTheme.tvColors: TvColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalTvColorScheme.current

val MaterialTheme.tvTypography: TvTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalTvTypography.current

@Composable
fun TvTheme(
    colors: TvColorScheme = TvColorScheme(),
    typography: TvTypography = TvTypography(),
    content: @Composable () -> Unit,
) {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        CompositionLocalProvider(
            LocalTvColorScheme provides colors,
            LocalTvTypography provides typography,
        ) {
            MaterialTheme(
                colorScheme = colors.toMaterialColorScheme(),
                typography = typography.toMaterialTypography(),
                content = content,
            )
        }
    }
}

private fun TvColorScheme.toMaterialColorScheme(): ColorScheme = darkColorScheme(
    primary = backgroundActive,
    onPrimary = textPrimaryActive,
    primaryContainer = backgroundActive,
    onPrimaryContainer = textPrimaryActive,
    secondary = backgroundActive20,
    onSecondary = textPrimary,
    secondaryContainer = backgroundOverlay,
    onSecondaryContainer = textPrimary,
    tertiary = backgroundBase,
    onTertiary = textPrimary,
    tertiaryContainer = backgroundBase,
    onTertiaryContainer = textPrimary,
    background = backgroundSunken,
    onBackground = textPrimary,
    surface = backgroundSurface,
    onSurface = textPrimary,
    surfaceVariant = backgroundOverlay,
    onSurfaceVariant = textSecondary,
    surfaceTint = backgroundActive,
    inverseSurface = backgroundActive,
    inverseOnSurface = textPrimaryActive,
    border = backgroundActive20,
    borderVariant = backgroundActive50,
)

private fun TvTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = title1,
    displayMedium = title2,
    displaySmall = title3,
    headlineLarge = title3,
    headlineMedium = headline,
    headlineSmall = callout,
    titleLarge = headline,
    titleMedium = subtitle1,
    titleSmall = callout,
    bodyLarge = caption1,
    bodyMedium = caption2,
    bodySmall = caption2,
    labelLarge = caption1,
    labelMedium = caption2,
    labelSmall = caption2,
)
