package au.com.shiftyjelly.pocketcasts.wear.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalColors
import au.com.shiftyjelly.pocketcasts.compose.PocketCastsTheme
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.themeTypeToColors
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun WearAppTheme(
    themeType: Theme.ThemeType,
    content: @Composable () -> Unit
) {
    val colors = themeTypeToColors(themeType)
    val isLight = !themeType.darkTheme
    val theme = PocketCastsTheme(colors = colors, isLight = isLight)

    CompositionLocalProvider(LocalColors provides theme) {
        // Using the wear.compose.material theme here
        // instead of the regular compose.material theme we use in the phone app
        MaterialTheme(
            colors = buildWearMaterialColors(colors),
            content = content
        )
    }
}

private fun buildWearMaterialColors(colors: ThemeColors): Colors {
    return Colors(
        primary = colors.primaryText01,
        primaryVariant = colors.primaryText01,
        secondary = colors.primaryText02,
        secondaryVariant = colors.primaryText02,
        background = colors.primaryUi04,
        surface = colors.primaryUi01,
        error = colors.support05,
        onPrimary = colors.primaryInteractive02,
        onSecondary = colors.primaryInteractive02,
        onBackground = colors.secondaryIcon01,
        onSurface = colors.primaryText01,
        onError = colors.secondaryIcon01,
    )
}
val MaterialTheme.theme: PocketCastsTheme
    @Composable
    @ReadOnlyComposable
    get() = LocalColors.current
