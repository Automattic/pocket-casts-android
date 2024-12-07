package au.com.shiftyjelly.pocketcasts.ui.helper

sealed interface StatusBarIconColor {
    // Dark color for status bar icons.
    data object Dark : StatusBarIconColor

    // Light color for status bar icons.
    data object Light : StatusBarIconColor

    // The icon color is determined by the theme.
    data object Theme : StatusBarIconColor

    // The icon color is determined by the theme, but the page has no toolbar or a toolbar the same color as the background.
    data object ThemeNoToolbar : StatusBarIconColor

    // The icon color for the Up Next either in the full-screen player or as a tab.
    data class UpNext(val isFullScreen: Boolean) : StatusBarIconColor
}
