package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.content.res.Resources
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPlural
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Singleton
class MultiSelectBookmarksHelper @Inject constructor(
    private val bookmarkManager: BookmarkManager,
) : MultiSelectHelper<Bookmark>() {
    override val maxToolbarIcons = 2

    override val toolbarActions: LiveData<List<MultiSelectAction>> = _selectedListLive
        .map {
            listOf(
                MultiSelectBookmarkAction.EditBookmark(isVisible = it.count() == 1),
                MultiSelectBookmarkAction.DeleteBookmark,
                MultiSelectAction.SelectAll,
            )
        }

    override lateinit var listener: Listener<Bookmark>

    override fun isSelected(multiSelectable: Bookmark) =
        selectedList.count { it.uuid == multiSelectable.uuid } > 0

    override fun onMenuItemSelected(
        itemId: Int,
        resources: Resources,
        fragmentManager: FragmentManager,
    ): Boolean {
        return when (itemId) {

            UR.id.menu_edit -> {
                // TODO: Bookmark - Add edit action
                true
            }

            R.id.menu_delete -> {
                delete(resources, fragmentManager)
                true
            }

            R.id.menu_select_all -> {
                selectAll()
                true
            }

            else -> false
        }
    }

    fun delete(resources: Resources, fragmentManager: FragmentManager) {
        val bookmarks = selectedList.toList()
        if (bookmarks.isEmpty()) {
            closeMultiSelect()
            return
        }

        val count = bookmarks.size
        ConfirmationDialog()
            .setForceDarkTheme(true)
            .setButtonType(
                ConfirmationDialog.ButtonType.Danger(
                    resources.getStringPlural(
                        count = count,
                        singular = LR.string.bookmarks_delete_singular,
                        plural = LR.string.bookmarks_delete_plural
                    )
                )
            )
            .setTitle(resources.getString(LR.string.are_you_sure))
            .setSummary(
                resources.getStringPlural(
                    count = count,
                    singular = LR.string.bookmarks_delete_summary_singular,
                    plural = LR.string.bookmarks_delete_summary_plural
                )
            )
            .setIconId(R.drawable.ic_delete)
            .setIconTint(UR.attr.support_05)
            .setOnConfirm {
                launch {
                    bookmarks.forEach {
                        bookmarkManager.deleteToSync(it.uuid)
                    }

                    withContext(Dispatchers.Main) {
                        val snackText = resources.getStringPlural(
                            count,
                            LR.string.bookmarks_deleted_singular,
                            LR.string.bookmarks_deleted_plural
                        )
                        showSnackBar(snackText)
                    }
                }
                closeMultiSelect()
            }
            .show(fragmentManager, "delete_bookmarks_warning")
    }
}
