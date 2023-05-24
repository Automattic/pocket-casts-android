package au.com.shiftyjelly.pocketcasts.wear.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    val typography = MaterialTheme.typography

    CompositionLocalProvider(LocalColors provides theme) {
        // Using the wear.compose.material theme here
        // instead of the regular compose.material theme we use in the phone app
        MaterialTheme(
            typography = typography.copy(
                display1 = typography.display1,
                display2 = typography.display2,
                display3 = typography.display3,
                title1 = typography.title1,
                title2 = typography.title2.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.2.sp
                ),
                title3 = typography.title3.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.sp
                ),
                body1 = typography.body1.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.sp
                ),
                body2 = typography.body2.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.sp
                ),
                button = typography.button.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.sp
                ),
                caption1 = typography.caption1,
                caption2 = typography.caption2,
                caption3 = typography.caption3
            ),
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
        onBackground = colors.secondaryIcon02,
        onSurface = colors.primaryText01,
        onError = colors.secondaryIcon01,
    )
}
val MaterialTheme.theme: PocketCastsTheme
    @Composable
    @ReadOnlyComposable
    get() = LocalColors.current
