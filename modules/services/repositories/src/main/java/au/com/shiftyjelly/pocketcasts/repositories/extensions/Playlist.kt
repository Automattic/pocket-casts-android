package au.com.shiftyjelly.pocketcasts.repositories.extensions

import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.images.R as IR

private const val FILTER_COLOR_SIZE = 5

private val ICON_DRAWABLES = listOf(
    IR.drawable.ic_filters_list,
    IR.drawable.ic_filters_headphones,
    IR.drawable.ic_filters_clock,
    IR.drawable.ic_filters_download,
    IR.drawable.ic_filters_play,
    IR.drawable.ic_filters_volume,
    IR.drawable.ic_filters_video,
    IR.drawable.ic_filters_star
)

private val SHORTCUT_DRAWABLES = listOf(
    IR.drawable.shortcut_list,
    IR.drawable.shortcut_headphones,
    IR.drawable.shortcut_clock,
    IR.drawable.shortcut_download,
    IR.drawable.shortcut_play,
    IR.drawable.shortcut_volume,
    IR.drawable.shortcut_video,
    IR.drawable.shortcut_star
)

private val AUTO_DRAWABLES = arrayOf(
    IR.drawable.auto_filter_list,
    IR.drawable.auto_filter_headphones,
    IR.drawable.auto_filter_clock,
    IR.drawable.auto_filter_downloaded,
    IR.drawable.auto_filter_play,
    IR.drawable.auto_filter_volume,
    IR.drawable.auto_filter_video,
    IR.drawable.auto_filter_star
)

private val AUTOMOTIVE_DRAWABLES = arrayOf(
    IR.drawable.automotive_filter_list,
    IR.drawable.automotive_filter_headphones,
    IR.drawable.automotive_filter_clock,
    IR.drawable.automotive_filter_downloaded,
    IR.drawable.automotive_filter_play,
    IR.drawable.automotive_filter_volume,
    IR.drawable.automotive_filter_video,
    IR.drawable.automotive_filter_star
)

val Playlist.drawableIndex: Int
    get() = iconId / FILTER_COLOR_SIZE % ICON_DRAWABLES.size

val Playlist.shortcutDrawableId: Int
    get() = SHORTCUT_DRAWABLES.getOrNull(drawableIndex) ?: SHORTCUT_DRAWABLES.first()

val Playlist.drawableId: Int
    get() = ICON_DRAWABLES.getOrNull(drawableIndex) ?: ICON_DRAWABLES.first()

val Playlist.autoDrawableId: Int
    get() = AUTO_DRAWABLES.getOrNull(drawableIndex) ?: AUTO_DRAWABLES.first()

val Playlist.automotiveDrawableId: Int
    get() = AUTOMOTIVE_DRAWABLES.getOrNull(drawableIndex) ?: AUTOMOTIVE_DRAWABLES.first()

val Playlist.colorIndex: Int
    get() = iconId % FILTER_COLOR_SIZE

val Playlist.Companion.iconDrawables: List<Int>
    get() = ICON_DRAWABLES

fun Playlist.Companion.calculateCombinedIconId(colorIndex: Int, iconIndex: Int): Int {
    return iconIndex * FILTER_COLOR_SIZE + colorIndex
}
