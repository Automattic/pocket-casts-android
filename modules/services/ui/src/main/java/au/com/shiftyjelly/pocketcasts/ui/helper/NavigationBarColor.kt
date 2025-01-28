package au.com.shiftyjelly.pocketcasts.ui.helper

import androidx.annotation.ColorInt
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

sealed interface NavigationBarColor {
    // Dark navigation bar with light icons.
    data object Dark : NavigationBarColor

    // Light navigation bar with dark icons.
    data object Light : NavigationBarColor

    // The navigation bar color is determined by the theme.
    data object Theme : NavigationBarColor

    // The navigation bar color for the full-screen player.
    data class Player(val podcast: Podcast?) : NavigationBarColor

    // The navigation bar color for the Up Next page.
    data class UpNext(val isFullScreen: Boolean) : NavigationBarColor

    // Dark navigation bar with light icons for a custom color.
    data class Color(@ColorInt val color: Int, val lightIcons: Boolean) : NavigationBarColor
}
