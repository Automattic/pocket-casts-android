package au.com.shiftyjelly.pocketcasts.ui.helper

sealed class StatusBarColor {
    object Dark : StatusBarColor()
    object Light : StatusBarColor()
    data class Custom(val color: Int, val isWhiteIcons: Boolean) : StatusBarColor()
}
