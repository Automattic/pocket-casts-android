package au.com.shiftyjelly.pocketcasts.player.view

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.type.BookmarksSortType
import au.com.shiftyjelly.pocketcasts.models.type.BookmarksSortTypeForPlayer
import au.com.shiftyjelly.pocketcasts.models.type.BookmarksSortTypeForPodcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class BookmarksSortByDialog(
    val settings: Settings,
    val changeSortOrder: (BookmarksSortType) -> Unit,
    private val sourceView: SourceView,
    private val forceDarkTheme: Boolean = false,
) {
    fun show(context: Context, fragmentManager: FragmentManager) {
        val message = "Bookmarks sort accessed in unknown source view: $sourceView"
        val sortOrder = when (sourceView) {
            SourceView.PLAYER -> settings.getBookmarksSortTypeForPlayer()
            SourceView.PODCAST_SCREEN -> settings.getBookmarksSortTypeForPodcast()
            else -> throw IllegalAccessError(message)
        }
        val dialog = OptionsDialog()
            .setTitle(context.resources.getString(LR.string.sort_by))
            .setForceDarkTheme(forceDarkTheme)
        val valuesToShow = when (sourceView) {
            SourceView.PLAYER -> BookmarksSortTypeForPlayer.values().toList()
            SourceView.PODCAST_SCREEN -> BookmarksSortTypeForPodcast.values().toList()
            else -> throw IllegalAccessError(message)
        }
        for (order in valuesToShow) {
            dialog.addCheckedOption(
                titleId = order.labelId,
                checked = order.labelId == sortOrder.labelId,
                click = { changeSortOrder(order) }
            )
        }
        dialog.show(fragmentManager, "bookmarks_sort_dialog")
    }
}
