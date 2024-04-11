package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortType
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeDefault
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeForPodcast
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeForProfile
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class BookmarksSortByDialog(
    val settings: Settings,
    val changeSortOrder: (BookmarksSortType) -> Unit,
    private val sourceView: SourceView,
    private val forceDarkTheme: Boolean = false,
) {
    fun show(context: Context, fragmentManager: FragmentManager) {
        val dialog = OptionsDialog()
            .setTitle(context.resources.getString(LR.string.sort_by))
            .setForceDarkTheme(forceDarkTheme)
        val sortOrder = sourceView.mapToBookmarksSortType()
        val valuesToShow = getValuesToShow()
        for (order in valuesToShow) {
            dialog.addCheckedOption(
                titleId = order.labelId,
                checked = order.key == sortOrder.key,
                click = { changeSortOrder(order) },
            )
        }
        dialog.show(fragmentManager, "bookmarks_sort_dialog")
    }

    private fun SourceView.mapToBookmarksSortType(): BookmarksSortType = when (this) {
        SourceView.PLAYER -> settings.playerBookmarksSortType.flow.value
        SourceView.PODCAST_SCREEN -> settings.podcastBookmarksSortType.flow.value
        SourceView.PROFILE -> settings.profileBookmarksSortType.flow.value
        else -> settings.episodeBookmarksSortType.flow.value
    }

    private fun getValuesToShow(): List<BookmarksSortType> = when (sourceView) {
        SourceView.PODCAST_SCREEN -> BookmarksSortTypeForPodcast.entries
        SourceView.PROFILE -> BookmarksSortTypeForProfile.entries
        else -> BookmarksSortTypeDefault.entries
    }
}
