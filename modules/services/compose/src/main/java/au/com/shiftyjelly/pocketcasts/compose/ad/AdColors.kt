package au.com.shiftyjelly.pocketcasts.compose.ad

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.compose.theme

data class AdColors(
    val bannerAd: Banner,
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
                    titleLabel = playerColors.highlight01,
                    adLabelBackground = playerColors.contrast06,
                    adLabel = playerColors.contrast01,
                    icon = playerColors.contrast02,
                    border = Color.Unspecified,
                    ripple = playerColors.contrast01,
                ),
            )
        } else {
            val themeColors = theme.colors
            AdColors(
                AdColors.Banner(
                    background = themeColors.primaryUi06,
                    ctaLabel = themeColors.primaryText01,
                    titleLabel = themeColors.primaryInteractive01,
                    adLabelBackground = themeColors.primaryInteractive01,
                    adLabel = themeColors.primaryUi02Active,
                    icon = themeColors.primaryIcon02,
                    border = themeColors.primaryUi05,
                    ripple = themeColors.primaryInteractive01,
                ),
            )
        }
    }
}
