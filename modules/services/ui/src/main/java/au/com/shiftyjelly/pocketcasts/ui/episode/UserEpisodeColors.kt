package au.com.shiftyjelly.pocketcasts.ui.episode

import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor

/**
 * Provides dark theme colors for user episodes.
 * Shared utility used across mobile, wear, and automotive modules.
 */
object UserEpisodeColors {
    data class ColorItem(
        val tintColorIndex: Int,
        val color: Int,
        val isGradient: Boolean,
    )

    fun darkThemeColors() = listOf(
        ColorItem(1, ThemeColor.primaryText02Dark, false),
        ColorItem(2, ThemeColor.filter01Dark, true),
        ColorItem(3, ThemeColor.filter05Dark, true),
        ColorItem(4, ThemeColor.filter04Dark, true),
        ColorItem(5, ThemeColor.filter03Dark, true),
        ColorItem(6, ThemeColor.filter02Dark, true),
        ColorItem(7, ThemeColor.filter06Dark, true),
        ColorItem(8, ThemeColor.filter07Dark, true),
    )
}
