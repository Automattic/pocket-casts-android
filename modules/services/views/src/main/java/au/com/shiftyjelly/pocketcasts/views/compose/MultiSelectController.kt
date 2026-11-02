package au.com.shiftyjelly.pocketcasts.views.compose

import android.content.res.Resources
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.asFlow
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MultiSelectController<T>(
    val helper: MultiSelectHelper<T>,
    scope: CoroutineScope,
    val sourceView: SourceView? = null,
) {
    val isMultiSelecting: StateFlow<Boolean> = helper.isMultiSelectingLive.asFlow()
        .stateIn(scope, SharingStarted.Eagerly, helper.isMultiSelecting)

    val selected: StateFlow<List<T>> = helper.selectedListLive.asFlow()
        .stateIn(scope, SharingStarted.Eagerly, helper.selectedListLive.value.orEmpty())

    val selectedCount: StateFlow<Int> = helper.selectedCount.asFlow()
        .stateIn(scope, SharingStarted.Eagerly, selected.value.size)

    val toolbarItems: StateFlow<MultiSelectToolbarItems> = helper.toolbarActions.asFlow()
        .map { actions -> multiSelectToolbarItems(actions, helper.maxToolbarIcons, overflowStyle, sourceView) }
        .stateIn(scope, SharingStarted.Eagerly, MultiSelectToolbarItems.Empty)

    private val overflowStyle
        get() = when (helper) {
            is MultiSelectEpisodesHelper -> MultiSelectOverflowStyle.BottomSheet
            is MultiSelectBookmarksHelper -> MultiSelectOverflowStyle.Menu
            else -> MultiSelectOverflowStyle.None
        }

    fun isSelected(item: T) = helper.isSelected(item)

    fun toggle(item: T) = helper.toggle(item)

    fun select(item: T) = helper.select(item)

    fun deselect(item: T) = helper.deselect(item)

    fun selectAll() = helper.selectAll()

    fun selectAllInList(items: List<T>) = helper.selectAllInList(items)

    fun deselectAllInList(items: List<T>) = helper.deselectAllInList(items)

    fun onLongPress(item: T, fragmentManager: FragmentManager, forceDarkTheme: Boolean = false) {
        helper.defaultLongPress(item, fragmentManager, forceDarkTheme)
    }

    fun onMenuItemSelected(itemId: Int, resources: Resources, activity: FragmentActivity): Boolean {
        return helper.onMenuItemSelected(itemId = itemId, resources = resources, activity = activity)
    }

    fun exit() {
        helper.isMultiSelecting = false
    }
}
