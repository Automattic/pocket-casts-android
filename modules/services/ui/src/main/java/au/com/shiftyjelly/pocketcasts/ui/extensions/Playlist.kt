package au.com.shiftyjelly.pocketcasts.ui.extensions

import android.content.Context
import android.graphics.Color
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.extensions.colorIndex
import au.com.shiftyjelly.pocketcasts.ui.R
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val filterThemeColors = listOf(
    R.attr.filter_01,
    R.attr.filter_05,
    R.attr.filter_04,
    R.attr.filter_06,
    R.attr.filter_03
)

fun Playlist.getStringForDuration(context: Context?): String {
    return when {
        context == null -> ""
        !filterDuration -> context.getString(LR.string.filters_duration)
        else -> {
            val longer = TimeHelper.getTimeDurationShortString(timeMs = (longerThan.toDouble() * 60000).toLong(), context = context)
            val shorter = TimeHelper.getTimeDurationShortString(timeMs = (shorterThan.toDouble() * 60000).toLong(), context = context)
            "$longer - $shorter"
        }
    }
}

val Playlist.Companion.themeColors: List<Int>
    get() = filterThemeColors

fun Playlist.getColor(context: Context?): Int {
    val themeColor = filterThemeColors.getOrNull(colorIndex) ?: return Color.WHITE
    return context?.getThemeColor(themeColor) ?: Color.WHITE
}

fun Playlist.Companion.getColors(context: Context?): List<Int> {
    return filterThemeColors.mapNotNull { context?.getThemeColor(it) }
}
