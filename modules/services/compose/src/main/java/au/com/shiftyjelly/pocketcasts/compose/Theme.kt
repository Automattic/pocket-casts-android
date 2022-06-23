package au.com.shiftyjelly.pocketcasts.compose

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

private val LocalColors = staticCompositionLocalOf { PocketCastsTheme(colors = ThemeLightColors, isLight = true) }

@Composable
fun AppTheme(
    themeType: Theme.ThemeType,
    content: @Composable () -> Unit
) {
    val colors = when (themeType) {
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

    val isLight = !themeType.darkTheme
    val theme = PocketCastsTheme(colors = colors, isLight = isLight)

    CompositionLocalProvider(LocalColors provides theme) {
        MaterialTheme(
            colors = buildMaterialColors(colors, isLight),
            content = content
        )
    }
}

data class PocketCastsTheme(
    val colors: ThemeColors,
    val isLight: Boolean
)

private fun buildMaterialColors(colors: ThemeColors, isLight: Boolean): Colors {
    return Colors(
        primary = colors.primaryInteractive01,
        primaryVariant = colors.primaryInteractive01,
        secondary = colors.primaryInteractive01,
        secondaryVariant = colors.primaryInteractive01,
        background = colors.primaryUi04,
        surface = colors.primaryUi01,
        error = colors.support05,
        onPrimary = colors.primaryInteractive02,
        onSecondary = colors.primaryInteractive02,
        onBackground = colors.secondaryIcon01,
        onSurface = colors.primaryInteractive01,
        onError = colors.secondaryIcon01,
        isLight = isLight
    )
}

val MaterialTheme.theme: PocketCastsTheme
    @Composable
    @ReadOnlyComposable
    get() = LocalColors.current
