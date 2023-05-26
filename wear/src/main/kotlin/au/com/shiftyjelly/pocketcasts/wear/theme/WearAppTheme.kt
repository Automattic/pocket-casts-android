package au.com.shiftyjelly.pocketcasts.wear.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

@Composable
fun WearAppTheme(
    content: @Composable () -> Unit
) {
    val typography = MaterialTheme.typography

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
        colors = buildWearMaterialColors(),
        content = content
    )
}

private fun buildWearMaterialColors(): Colors {
    val backgroundColor = Color.Black
    val surfaceColor = WearColors.surface
    val primaryTextColor = WearColors.primaryText
    val secondaryTextColor = WearColors.secondaryText
    return Colors(
        primary = surfaceColor,
        primaryVariant = surfaceColor,
        secondary = surfaceColor,
        secondaryVariant = surfaceColor,
        background = backgroundColor,
        surface = surfaceColor,
        error = WearColors.highlight,
        onPrimary = primaryTextColor,
        onSecondary = secondaryTextColor,
        onBackground = primaryTextColor,
        onSurface = primaryTextColor,
        onError = primaryTextColor,
    )
}
