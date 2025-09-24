package au.com.shiftyjelly.pocketcasts.repositories.extensions

import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistIcon
import au.com.shiftyjelly.pocketcasts.images.R as IR

private const val COLORS_COUNT = 5

private val drawables = listOf(
    IR.drawable.ic_filters_list,
    IR.drawable.ic_filters_headphones,
    IR.drawable.ic_filters_clock,
    IR.drawable.ic_filters_download,
    IR.drawable.ic_filters_play,
    IR.drawable.ic_filters_volume,
    IR.drawable.ic_filters_video,
    IR.drawable.ic_filters_star,
)

private val shortcutDrawables = listOf(
    IR.drawable.shortcut_list,
    IR.drawable.shortcut_headphones,
    IR.drawable.shortcut_clock,
    IR.drawable.shortcut_download,
    IR.drawable.shortcut_play,
    IR.drawable.shortcut_volume,
    IR.drawable.shortcut_video,
    IR.drawable.shortcut_star,
)

private val autoDrawables = listOf(
    IR.drawable.auto_filter_list,
    IR.drawable.auto_filter_headphones,
    IR.drawable.auto_filter_clock,
    IR.drawable.auto_filter_downloaded,
    IR.drawable.auto_filter_play,
    IR.drawable.auto_filter_volume,
    IR.drawable.auto_filter_video,
    IR.drawable.auto_filter_star,
)

private val automotiveDrawables = listOf(
    IR.drawable.automotive_filter_list,
    IR.drawable.automotive_filter_headphones,
    IR.drawable.automotive_filter_clock,
    IR.drawable.automotive_filter_downloaded,
    IR.drawable.automotive_filter_play,
    IR.drawable.automotive_filter_volume,
    IR.drawable.automotive_filter_video,
    IR.drawable.automotive_filter_star,
)

val PlaylistIcon.drawableIndex: Int
    get() = id / COLORS_COUNT % drawables.size

val PlaylistIcon.drawableId: Int
    get() = drawables.getOrNull(drawableIndex) ?: drawables.first()

val PlaylistIcon.shortcutDrawableId: Int
    get() = shortcutDrawables.getOrNull(drawableIndex) ?: shortcutDrawables.first()

val PlaylistIcon.autoDrawableId: Int
    get() = autoDrawables.getOrNull(drawableIndex) ?: autoDrawables.first()

val PlaylistIcon.automotiveDrawableId: Int
    get() = automotiveDrawables.getOrNull(drawableIndex) ?: automotiveDrawables.first()

val PlaylistIcon.colorIndex: Int
    get() = id % COLORS_COUNT

val PlaylistEntity.Companion.iconDrawables: List<Int>
    get() = drawables

fun PlaylistEntity.Companion.calculatePlaylistIcon(colorIndex: Int, iconIndex: Int): PlaylistIcon {
    return PlaylistIcon(iconIndex * COLORS_COUNT + colorIndex)
}
