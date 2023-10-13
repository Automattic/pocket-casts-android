package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.content.Context
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortType
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeDefault
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeForPodcast
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
                click = { changeSortOrder(order) }
            )
        }
        dialog.show(fragmentManager, "bookmarks_sort_dialog")
    }

    private fun SourceView.mapToBookmarksSortType(): BookmarksSortType = when (this) {
        SourceView.FILES,
        SourceView.EPISODE_DETAILS,
        -> settings.episodeBookmarksSortType.flow.value

        SourceView.PLAYER -> settings.playerBookmarksSortType.flow.value
        SourceView.PODCAST_SCREEN -> settings.podcastBookmarksSortType.flow.value
        else -> throw IllegalAccessError("$UNKNOWN_SOURCE_MESSAGE $this")
    }

    private fun getValuesToShow(): List<BookmarksSortType> = when (sourceView) {
        SourceView.FILES,
        SourceView.EPISODE_DETAILS,
        SourceView.PLAYER -> BookmarksSortTypeDefault.values().toList()
        SourceView.PODCAST_SCREEN -> BookmarksSortTypeForPodcast.values().toList()
        else -> throw IllegalAccessError("$UNKNOWN_SOURCE_MESSAGE $sourceView")
    }

    companion object {
        private const val UNKNOWN_SOURCE_MESSAGE = "Bookmarks sort accessed in unknown source view:"
    }
}
