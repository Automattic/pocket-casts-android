package au.com.shiftyjelly.pocketcasts.ui.extensions

import android.content.Context
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistIcon
import au.com.shiftyjelly.pocketcasts.repositories.extensions.colorIndex
import au.com.shiftyjelly.pocketcasts.ui.R
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val colors = listOf(
    R.attr.filter_01,
    R.attr.filter_05,
    R.attr.filter_04,
    R.attr.filter_06,
    R.attr.filter_03,
)

fun PlaylistIcon.getColor(context: Context): Int {
    return context.getThemeColor(colors[colorIndex])
}

fun PlaylistEntity.Companion.getColors(context: Context): List<Int> {
    return colors.map(context::getThemeColor)
}

val PlaylistEntity.Companion.themeColors: List<Int>
    get() = colors

fun PlaylistEntity.getStringForDuration(context: Context?): String {
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
