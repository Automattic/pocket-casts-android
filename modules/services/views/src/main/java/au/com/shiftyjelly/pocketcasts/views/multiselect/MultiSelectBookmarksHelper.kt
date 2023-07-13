package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.content.res.Resources
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.views.R
import javax.inject.Inject
import javax.inject.Singleton
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Singleton
class MultiSelectBookmarksHelper @Inject constructor() : MultiSelectHelper<Bookmark>() {

    override val toolbarActions: LiveData<List<MultiSelectAction>> = selectedListLive
        .map {
            listOf(
                MultiSelectAction.BookmarkAction.EditBookmark(isVisible = it.count() == 1),
                MultiSelectAction.BookmarkAction.DeleteBookmark,
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
                // TODO: Bookmark - Add delete action
                true
            }

            R.id.menu_select_all -> {
                selectAll()
                true
            }

            R.id.menu_unselect_all -> {
                unselectAll()
                true
            }

            else -> false
        }
    }
}
