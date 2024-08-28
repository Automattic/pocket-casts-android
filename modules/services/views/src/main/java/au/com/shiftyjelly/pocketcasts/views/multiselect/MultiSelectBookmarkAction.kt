package au.com.shiftyjelly.pocketcasts.views.multiselect

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

sealed class MultiSelectBookmarkAction(
    override val groupId: String,
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
    isVisible,
) {
    data object DeleteBookmark : MultiSelectBookmarkAction(
        groupId = "delete",
        actionId = R.id.menu_delete,
        title = LR.string.delete,
        iconRes = R.drawable.ic_delete,
        analyticsValue = "delete",
    )

    data class EditBookmark(override val isVisible: Boolean) : MultiSelectBookmarkAction(
        groupId = "edit",
        actionId = UR.id.menu_edit,
        title = LR.string.edit,
        iconRes = IR.drawable.ic_edit,
        analyticsValue = "edit",
        isVisible = isVisible,
    )

    data class ShareBookmark(override val isVisible: Boolean) : MultiSelectBookmarkAction(
        groupId = "share",
        actionId = R.id.menu_share,
        title = LR.string.share,
        iconRes = IR.drawable.ic_share,
        analyticsValue = "share",
    )
}
