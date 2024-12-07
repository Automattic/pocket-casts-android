package au.com.shiftyjelly.pocketcasts.ui.helper

sealed interface NavigationBarIconColor {
    // Dark color for status bar icons.
    data object Dark : NavigationBarIconColor

    // Light color for status bar icons.
    data object Light : NavigationBarIconColor

    // The icon color is determined by the theme.
    data object Theme : NavigationBarIconColor
}
