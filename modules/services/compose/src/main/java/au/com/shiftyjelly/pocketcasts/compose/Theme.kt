package au.com.shiftyjelly.pocketcasts.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

val LocalColors = staticCompositionLocalOf { PocketCastsTheme(type = Theme.ThemeType.LIGHT, colors = ThemeLightColors) }

/**
 * This theme should be used to support light/dark colors if the composable root of the view tree
 * does not support the use of contentColor.
 * @see <a href="https://developer.android.com/jetpack/compose/themes/material#content-color</a> for more details
 */
@Suppress("ktlint:compose:modifier-missing-check")
@Composable
fun AppThemeWithBackground(
    themeType: Theme.ThemeType,
    content: @Composable () -> Unit,
) {
    AppTheme(themeType) {
        // Use surface so Material uses appropraite tinting for icons etc.
        Surface(color = MaterialTheme.colors.background) {
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
            colors = buildMaterialColors(colors, theme.isLight),
            content = content,
        )
    }
}

@Composable
fun themeTypeToColors(themeType: Theme.ThemeType) = when (themeType) {
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
            colors = buildMaterialColors(colors = theme.colors, isLight = theme.isLight),
            typography = typography.copy(
                h1 = typography.h1.copy(fontSize = 144.sp),
                h2 = typography.h2.copy(fontSize = 90.sp),
                h3 = typography.h3.copy(fontSize = 72.sp),
                h4 = typography.h4.copy(fontSize = 51.sp),
                h5 = typography.h5.copy(fontSize = 36.sp),
                h6 = typography.h6.copy(fontSize = 30.sp),
                body1 = typography.body1.copy(fontSize = 24.sp),
                body2 = typography.body2.copy(fontSize = 21.sp),
                subtitle1 = typography.subtitle1.copy(fontSize = 24.sp),
                subtitle2 = typography.subtitle2.copy(fontSize = 21.sp),
                button = typography.button.copy(fontSize = 21.sp),
                caption = typography.caption.copy(fontSize = 18.sp),
                overline = typography.overline.copy(fontSize = 15.sp),
            ),
            content = content,
        )
    }
}

private val radioactiveColorFilter = ColorFilter.tint(
    color = Color.radioactiveGreen,
    blendMode = BlendMode.Modulate,
)

data class PocketCastsTheme(
    val type: Theme.ThemeType,
    val colors: ThemeColors,
) {
    val isDark get() = type.darkTheme
    val isLight get() = !isDark
    val imageColorFilter get() = if (type == Theme.ThemeType.RADIOACTIVE) radioactiveColorFilter else null

    @Composable
    fun rememberPlayerColors(): PlayerColors? {
        return LocalPodcastColors.current?.let { podcastColors ->
            rememberPlayerColors(podcastColors)
        }
    }

    @Composable
    fun rememberPlayerColorsOrDefault(): PlayerColors {
        return rememberPlayerColors(LocalPodcastColors.current ?: PodcastColors.ForUserEpisode)
    }

    @Composable
    private fun rememberPlayerColors(podcastColors: PodcastColors): PlayerColors {
        return remember(podcastColors) { PlayerColors(type, podcastColors) }
    }
}

@SuppressLint("ConflictingOnColor")
private fun buildMaterialColors(colors: ThemeColors, isLight: Boolean): Colors {
    return Colors(
        primary = colors.primaryInteractive01,
        primaryVariant = colors.primaryInteractive01,
        secondary = colors.primaryInteractive01,
        secondaryVariant = colors.primaryInteractive01,
        background = colors.primaryUi01,
        surface = colors.primaryUi01,
        error = colors.support05,
        onPrimary = colors.primaryInteractive02,
        onSecondary = colors.primaryInteractive02,
        onBackground = colors.secondaryIcon01,
        onSurface = colors.primaryInteractive01,
        onError = colors.secondaryIcon01,
        isLight = isLight,
    )
}

val MaterialTheme.theme: PocketCastsTheme
    @Composable
    @ReadOnlyComposable
    get() = LocalColors.current
