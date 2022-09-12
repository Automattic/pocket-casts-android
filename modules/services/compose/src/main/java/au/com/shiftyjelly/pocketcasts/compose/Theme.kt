package au.com.shiftyjelly.pocketcasts.compose

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

private val LocalColors = staticCompositionLocalOf { PocketCastsTheme(colors = ThemeLightColors, isLight = true) }

/**
 * This theme should be used to support light/dark colors if the composable root of the view tree
 * does not support the use of contentColor.
 * @see <a href="https://developer.android.com/jetpack/compose/themes/material#content-color</a> for more details
 */
@Composable
fun AppThemeWithBackground(
    themeType: Theme.ThemeType,
    content: @Composable () -> Unit
) {
    AppTheme(themeType) {
        SurfacedContent(content)
    }
}

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

@Composable
private fun SurfacedContent(
    content: @Composable () -> Unit
) {
    Surface(color = MaterialTheme.colors.background) {
        content()
    }
}

private fun increaseFontSize(textStyle: TextStyle, times: Double): TextStyle {
    return textStyle.copy(fontSize = textStyle.fontSize.times(times))
}

@Composable
fun AutomotiveTheme(content: @Composable () -> Unit) {
    val theme = PocketCastsTheme(colors = ThemeDarkColors, isLight = false)
    val typography = MaterialTheme.typography
    val fontIncrease = 1.5
    CompositionLocalProvider(LocalColors provides theme) {
        MaterialTheme(
            colors = buildMaterialColors(colors = theme.colors, isLight = theme.isLight),
            typography = typography.copy(
                h1 = increaseFontSize(typography.h1, fontIncrease),
                h2 = increaseFontSize(typography.h2, fontIncrease),
                h3 = increaseFontSize(typography.h3, fontIncrease),
                h4 = increaseFontSize(typography.h4, fontIncrease),
                h5 = increaseFontSize(typography.h5, fontIncrease),
                h6 = increaseFontSize(typography.h6, fontIncrease),
                body1 = increaseFontSize(typography.body1, fontIncrease),
                body2 = increaseFontSize(typography.body2, fontIncrease),
                subtitle1 = increaseFontSize(typography.subtitle1, fontIncrease),
                subtitle2 = increaseFontSize(typography.subtitle2, fontIncrease),
                button = increaseFontSize(typography.button, fontIncrease),
                caption = increaseFontSize(typography.caption, fontIncrease),
                overline = increaseFontSize(typography.overline, fontIncrease)
            ),
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
