package au.com.shiftyjelly.pocketcasts.compose.ad

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils

data class AdColors(
    val bannerAd: Banner,
    val reportSheet: ReportSheet,
) {
    data class Banner(
        val background: Color,
        val ctaLabel: Color,
        val titleLabel: Color,
        val adLabelBackground: Color,
        val adLabel: Color,
        val icon: Color,
        val border: Color,
        val ripple: Color,
    )

    data class ReportSheet(
        val surface: Color,
        val primaryText: Color,
        val highlightText: Color,
        val icon: Color,
        val ripple: Color,
        val divider: Color,
    )
}

@Composable
fun rememberAdColors(): AdColors {
    val theme = MaterialTheme.theme
    val playerColors = theme.rememberPlayerColors()
    return remember(theme.type, playerColors) {
        if (playerColors != null) {
            AdColors(
                AdColors.Banner(
                    background = playerColors.contrast06,
                    ctaLabel = playerColors.contrast01,
                    titleLabel = playerColors.contrast01,
                    adLabelBackground = playerColors.contrast06,
                    adLabel = playerColors.contrast01,
                    icon = playerColors.contrast02,
                    border = Color.Unspecified,
                    ripple = playerColors.contrast01,
                ),
                AdColors.ReportSheet(
                    surface = playerColors.background01,
                    primaryText = playerColors.contrast01,
                    highlightText = playerColors.contrast01,
                    icon = playerColors.contrast01,
                    ripple = playerColors.contrast01,
                    divider = playerColors.contrast05,
                ),
            )
        } else {
            val themeColors = theme.colors
            AdColors(
                AdColors.Banner(
                    background = themeColors.primaryUi01,
                    ctaLabel = themeColors.primaryText01,
                    titleLabel = themeColors.primaryInteractive01,
                    adLabelBackground = themeColors.primaryInteractive01,
                    adLabel = themeColors.primaryUi02Active,
                    icon = themeColors.primaryIcon02,
                    border = themeColors.primaryUi05,
                    ripple = themeColors.primaryInteractive01,
                ),
                AdColors.ReportSheet(
                    surface = themeColors.primaryUi01,
                    primaryText = themeColors.primaryText01,
                    highlightText = themeColors.support01,
                    icon = themeColors.primaryIcon03,
                    ripple = themeColors.primaryInteractive01,
                    divider = themeColors.primaryUi05,
                ),
            )
        }
    }
}
