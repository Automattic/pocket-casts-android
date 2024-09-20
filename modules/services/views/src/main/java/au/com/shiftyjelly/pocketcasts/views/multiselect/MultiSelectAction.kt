package au.com.shiftyjelly.pocketcasts.views.multiselect

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

sealed class MultiSelectAction(
    open val groupId: String,
    open val actionId: Int,
    @StringRes open val title: Int,
    @DrawableRes open val iconRes: Int,
    open val analyticsValue: String,
    open val isVisible: Boolean = true,
) {
    object SelectAll : MultiSelectAction(
        groupId = "select_all",
        actionId = R.id.menu_select_all,
        title = LR.string.select_all,
        iconRes = IR.drawable.ic_selectall_up,
        analyticsValue = "select_all",
    )
}
