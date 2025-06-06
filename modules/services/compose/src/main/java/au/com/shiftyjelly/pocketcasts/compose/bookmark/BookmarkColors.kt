package au.com.shiftyjelly.pocketcasts.compose.bookmark

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import au.com.shiftyjelly.pocketcasts.compose.buttons.TimePlayButtonColors
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBannerColors
import au.com.shiftyjelly.pocketcasts.compose.theme

data class BookmarkColors(
    val bookmarkRow: BookmarkRowColors,
    val headerRow: HeaderRowColors,
    val playButton: TimePlayButtonColors,
    val noContent: NoContentBannerColors,
)

@Composable
fun rememberBookmarkColors(): BookmarkColors {
    val theme = MaterialTheme.theme
    val playerColors = theme.rememberPlayerColors()

    return remember(theme.type, playerColors) {
        if (playerColors != null) {
            BookmarkColors(
                bookmarkRow = BookmarkRowColors.player(playerColors),
                playButton = TimePlayButtonColors.player(playerColors),
                headerRow = HeaderRowColors.player(playerColors),
                noContent = NoContentBannerColors.player(playerColors),
            )
        } else {
            val themeColors = theme.colors
            BookmarkColors(
                bookmarkRow = BookmarkRowColors.default(themeColors),
                playButton = TimePlayButtonColors.default(themeColors),
                headerRow = HeaderRowColors.default(themeColors),
                noContent = NoContentBannerColors.default(themeColors),
            )
        }
    }
}
