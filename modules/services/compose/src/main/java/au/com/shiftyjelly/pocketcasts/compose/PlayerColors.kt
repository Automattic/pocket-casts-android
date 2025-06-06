package au.com.shiftyjelly.pocketcasts.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor

data class PlayerColors(
    val theme: Theme.ThemeType,
    val podcastColors: PodcastColors,
) {
    val background01 = Color(ThemeColor.playerBackground01(theme, podcastColors.background.toArgb()))

    val background02 = Color(ThemeColor.playerBackground02(theme, podcastColors.background.toArgb()))

    val highlight01 = Color(ThemeColor.playerHighlight01(theme, podcastColors.playerTint.toArgb()))

    val highlight02 = Color(ThemeColor.playerHighlight02(theme, podcastColors.playerTint.toArgb()))

    val highlight03 = Color(ThemeColor.playerHighlight03(theme, podcastColors.playerTint.toArgb()))

    val highlight04 = Color(ThemeColor.playerHighlight04(theme, podcastColors.playerTint.toArgb()))

    val highlight05 = Color(ThemeColor.playerHighlight05(theme, podcastColors.playerTint.toArgb()))

    val highlight06 = Color(ThemeColor.playerHighlight06(theme, podcastColors.playerTint.toArgb()))

    val highlight07 = Color(ThemeColor.playerHighlight07(theme, podcastColors.playerTint.toArgb()))

    val contrast01 = Color(ThemeColor.playerContrast01(theme))

    val contrast02 = Color(ThemeColor.playerContrast02(theme))

    val contrast03 = Color(ThemeColor.playerContrast03(theme))

    val contrast04 = Color(ThemeColor.playerContrast04(theme))

    val contrast05 = Color(ThemeColor.playerContrast05(theme))

    val contrast06 = Color(ThemeColor.playerContrast06(theme))
}
