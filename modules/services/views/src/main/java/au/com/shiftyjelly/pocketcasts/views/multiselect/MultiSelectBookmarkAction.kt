package au.com.shiftyjelly.pocketcasts.views.multiselect

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

sealed class MultiSelectBookmarkAction(
    override val groupId: Int,
    override val actionId: Int,
    @StringRes override val title: Int,
    @DrawableRes override val iconRes: Int,
    override val analyticsValue: String,
    override val isVisible: Boolean = true,
) : MultiSelectAction(
    groupId,
    actionId,
    title,
    iconRes,
    analyticsValue,
    isVisible
) {
    object DeleteBookmark : MultiSelectBookmarkAction(
        R.id.menu_delete,
        R.id.menu_delete,
        LR.string.delete,
        R.drawable.ic_delete,
        "delete",
    )

    data class EditBookmark(override val isVisible: Boolean) : MultiSelectBookmarkAction(
        UR.id.menu_edit,
        UR.id.menu_edit,
        LR.string.delete,
        IR.drawable.ic_edit,
        "edit",
        isVisible = isVisible
    )
}
