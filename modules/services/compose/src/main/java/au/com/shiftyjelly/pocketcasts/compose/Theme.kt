package au.com.shiftyjelly.pocketcasts.compose

import android.annotation.SuppressLint
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

val LocalColors = staticCompositionLocalOf { PocketCastsTheme(type = Theme.ThemeType.LIGHT, colors = ThemeLightColors) }

/**
 * This theme should be used to support light/dark colors if the composable root of the view tree
 * does not support the use of contentColor.
 * @see <a href="https://developer.android.com/jetpack/compose/themes/material#content-color</a> for more details
 */
@Composable
fun AppThemeWithBackground(
    themeType: Theme.ThemeType,
    content: @Composable () -> Unit,
) {
    AppTheme(themeType) {
        // Use surface so Material uses appropraite tinting for icons etc.
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

@Composable
fun AppTheme(
    themeType: Theme.ThemeType,
    content: @Composable () -> Unit,
) {
    val colors = themeTypeToColors(themeType)
    val theme = PocketCastsTheme(type = themeType, colors = colors)

    CompositionLocalProvider(LocalColors provides theme) {
        MaterialTheme(
            colorScheme = buildMaterialColors(colors, theme.isLight),
            content = content,
        )
    }
}

@Composable
fun themeTypeToColors(themeType: Theme.ThemeType) =
    when (themeType) {
        Theme.ThemeType.LIGHT -> ThemeLightColors
        Theme.ThemeType.DARK -> ThemeDarkColors
        Theme.ThemeType.EXTRA_DARK -> ThemeExtraDarkColors
        Theme.ThemeType.CLASSIC_LIGHT -> ThemeClassicLightColors
        Theme.ThemeType.ELECTRIC -> ThemeElectricityColors
        Theme.ThemeType.INDIGO -> ThemeIndigoColors
        Theme.ThemeType.RADIOACTIVE -> ThemeRadioactiveColors
        Theme.ThemeType.ROSE -> ThemeRoseColors
        Theme.ThemeType.LIGHT_CONTRAST -> ThemeLightContrastColors
        Theme.ThemeType.DARK_CONTRAST -> ThemeDarkContrastColors
    }

@Composable
fun AutomotiveTheme(content: @Composable () -> Unit) {
    val theme = PocketCastsTheme(type = Theme.ThemeType.DARK, colors = ThemeDarkColors)
    val typography = MaterialTheme.typography
    // Increase the size of the fonts on Automotive to match the system
    CompositionLocalProvider(LocalColors provides theme) {
        MaterialTheme(
            colorScheme = buildMaterialColors(colors = theme.colors, isLight = theme.isLight),
            typography = Typography(
                displayLarge = typography.displayLarge.copy(fontSize = 144.sp),
                displayMedium = typography.displayMedium.copy(fontSize = 90.sp),
                displaySmall = typography.displaySmall.copy(fontSize = 72.sp),
                headlineLarge = typography.headlineLarge.copy(fontSize = 51.sp),
                headlineMedium = typography.headlineMedium.copy(fontSize = 36.sp),
                headlineSmall = typography.headlineSmall.copy(fontSize = 30.sp),
                bodyLarge = typography.bodyLarge.copy(fontSize = 24.sp),
                bodyMedium = typography.bodyMedium.copy(fontSize = 21.sp),
                bodySmall = typography.bodySmall.copy(fontSize = 18.sp),
                titleLarge = typography.titleLarge.copy(fontSize = 24.sp),
                titleMedium = typography.titleMedium.copy(fontSize = 21.sp),
                titleSmall = typography.titleSmall.copy(fontSize = 18.sp),
                labelLarge = typography.labelLarge.copy(fontSize = 21.sp),
                labelMedium = typography.labelMedium.copy(fontSize = 18.sp),
                labelSmall = typography.labelSmall.copy(fontSize = 15.sp),
            ),
            content = content,
        )
    }
}

data class PocketCastsTheme(
    val type: Theme.ThemeType,
    val colors: ThemeColors,
) {
    val isDark get() = type.darkTheme
    val isLight get() = !isDark
}

@SuppressLint("ConflictingOnColor")
private fun buildMaterialColors(colors: ThemeColors, isLight: Boolean): ColorScheme {
    return ColorScheme(
        primary = colors.primaryInteractive01,
        onPrimary = colors.primaryInteractive02,
        primaryContainer = colors.primaryUi02,
        onPrimaryContainer = colors.primaryText01,
        inversePrimary = colors.primaryInteractive02,
        secondary = colors.primaryInteractive01,
        onSecondary = colors.primaryInteractive02,
        secondaryContainer = colors.primaryUi02,
        onSecondaryContainer = colors.primaryText01,
        tertiary = colors.primaryInteractive03,
        onTertiary = colors.primaryText02,
        tertiaryContainer = colors.primaryUi03,
        onTertiaryContainer = colors.primaryText02,
        background = colors.primaryUi01,
        onBackground = colors.secondaryIcon01,
        surface = colors.primaryUi01,
        onSurface = colors.primaryInteractive01,
        surfaceVariant = colors.primaryUi02,
        onSurfaceVariant = colors.primaryText02,
        surfaceTint = colors.primaryInteractive01,
        inverseSurface = colors.primaryUi01, // if (isLight) colors.primaryUi05 else colors.primaryUi01,
        inverseOnSurface = colors.primaryText01, // if (isLight) colors.primaryText01Light else colors.primaryText01,
        error = colors.support05,
        onError = colors.secondaryIcon01,
        errorContainer = colors.support05.copy(alpha = 0.2f),
        onErrorContainer = colors.support05,
        outline = colors.primaryField01,
        outlineVariant = colors.primaryField02,
        scrim = colors.primaryUi05.copy(alpha = 0.5f),
        surfaceBright = colors.primaryUi01,
        surfaceDim = colors.primaryUi05,
        surfaceContainer = colors.primaryUi02,
        surfaceContainerHigh = colors.primaryUi03,
        surfaceContainerHighest = colors.primaryUi04,
        surfaceContainerLow = colors.primaryUi01,
        surfaceContainerLowest = colors.primaryUi01,
    )
}

val MaterialTheme.theme: PocketCastsTheme
    @Composable
    @ReadOnlyComposable
    get() = LocalColors.current
