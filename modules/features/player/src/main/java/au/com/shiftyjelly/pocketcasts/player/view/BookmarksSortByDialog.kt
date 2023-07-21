package au.com.shiftyjelly.pocketcasts.player.view

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.models.type.BookmarksSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class BookmarksSortByDialog(
    val settings: Settings,
    val changeSortOrder: (BookmarksSortType) -> Unit
) {
    fun show(context: Context, fragmentManager: FragmentManager) {
        val sortOrder = settings.getBookmarksSortType()
        val dialog = OptionsDialog()
            .setTitle(context.resources.getString(LR.string.sort_by))
            .setForceDarkTheme(true)
        for (order in BookmarksSortType.values()) {
            dialog.addCheckedOption(
                titleId = order.labelId,
                checked = order.labelId == sortOrder.labelId,
                click = { changeSortOrder(order) }
            )
        }
        dialog.show(fragmentManager, "bookmarks_sort_dialog")
    }
}
