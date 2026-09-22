package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.content.res.Resources
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPlural
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import com.automattic.eventhorizon.BookmarkDeleteFormDismissedEvent
import com.automattic.eventhorizon.BookmarkDeleteFormShownEvent
import com.automattic.eventhorizon.BookmarkDeleteFormSubmittedEvent
import com.automattic.eventhorizon.BookmarkDeletedEvent
import com.automattic.eventhorizon.EventHorizon
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class BookmarkDeleter @Inject constructor(
    private val bookmarkManager: BookmarkManager,
    private val eventHorizon: EventHorizon,
) {
    fun confirmDelete(
        bookmarks: List<Bookmark>,
        source: SourceView,
        resources: Resources,
        fragmentManager: FragmentManager,
        scope: CoroutineScope,
        onConfirmed: () -> Unit = {},
        onDeleted: (Int) -> Unit = {},
        onDismissed: () -> Unit = {},
    ) {
        if (bookmarks.isEmpty()) {
            onConfirmed()
            return
        }
        eventHorizon.track(BookmarkDeleteFormShownEvent(source = source.analyticsValue))

        val count = bookmarks.size
        ConfirmationDialog()
            .setForceDarkTheme(source == SourceView.PLAYER)
            .setButtonType(
                ConfirmationDialog.ButtonType.Danger(
                    resources.getStringPlural(
                        count = count,
                        singular = LR.string.bookmarks_delete_singular,
                        plural = LR.string.bookmarks_delete_plural,
                    ),
                ),
            )
            .setTitle(resources.getString(LR.string.are_you_sure))
            .setSummary(
                resources.getStringPlural(
                    count = count,
                    singular = LR.string.bookmarks_delete_summary_singular,
                    plural = LR.string.bookmarks_delete_summary_plural,
                ),
            )
            .setIconId(R.drawable.ic_delete)
            .setIconTint(UR.attr.support_05)
            .setOnConfirm {
                scope.launch {
                    deleteConfirmed(bookmarks, source)
                    withContext(Dispatchers.Main) { onDeleted(count) }
                }
                onConfirmed()
            }
            .setOnDismiss { withoutAction ->
                if (withoutAction) {
                    eventHorizon.track(BookmarkDeleteFormDismissedEvent(source = source.analyticsValue))
                    onDismissed()
                }
            }
            .show(fragmentManager, "delete_bookmarks_warning")
    }

    internal suspend fun deleteConfirmed(bookmarks: List<Bookmark>, source: SourceView) {
        if (bookmarks.isEmpty()) {
            return
        }
        eventHorizon.track(BookmarkDeleteFormSubmittedEvent(source = source.analyticsValue))
        bookmarks.forEach { bookmark ->
            bookmarkManager.deleteToSync(bookmark.uuid)
            eventHorizon.track(BookmarkDeletedEvent(source = source.analyticsValue))
        }
    }
}
