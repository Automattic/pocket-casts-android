package au.com.shiftyjelly.pocketcasts.views.multiselect

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.MultiSelectViewOverflowMenuRearrangeActionMovedEvent
import com.automattic.eventhorizon.MultiSelectViewOverflowMenuRearrangeFinishedEvent
import com.automattic.eventhorizon.ShelfActionSourceType
import java.util.Collections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal data class MultiSelectRearrangeTitle(@StringRes val title: Int)

internal class MultiSelectRearrangeState(
    private val settings: Settings,
    private val eventHorizon: EventHorizon,
    private val maxToolbarIcons: Int,
    private val source: SourceView,
    private val shouldShowRemoveListeningHistory: Boolean,
) {
    private val shortcutTitle = MultiSelectRearrangeTitle(LR.string.multiselect_actions_shown)
    private val overflowTitle = MultiSelectRearrangeTitle(LR.string.multiselect_actions_hidden)
    private var dragStartPosition: Int? = null

    private val _items = MutableStateFlow<List<Any>>(emptyList())
    val items: StateFlow<List<Any>> = _items.asStateFlow()

    fun onSettingsChanged(groupIds: List<String>) {
        val actions: MutableList<Any> = MultiSelectEpisodeAction.listFromIds(groupIds).toMutableList()
        if (!shouldShowRemoveListeningHistory) {
            actions.removeAll { it is MultiSelectEpisodeAction.RemoveListeningHistory }
        }
        actions.add(0, shortcutTitle)
        actions.add(maxToolbarIcons + 1, overflowTitle)
        _items.value = actions.toList()
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        val listData = _items.value.toMutableList()
        if (fromPosition < toPosition) {
            for (index in fromPosition until toPosition) {
                Collections.swap(listData, index, index + 1)
            }
        } else {
            for (index in fromPosition downTo toPosition + 1) {
                Collections.swap(listData, index, index - 1)
            }
        }
        listData.remove(shortcutTitle)
        listData.remove(overflowTitle)
        listData.add(maxToolbarIcons + 1, overflowTitle)
        listData.add(0, shortcutTitle)
        _items.value = listData.toList()
    }

    fun onItemStartDrag(position: Int) {
        dragStartPosition = position
    }

    fun onItemTouchFinished(position: Int) {
        val items = _items.value
        settings.setMultiSelectItems(items.filterIsInstance<MultiSelectAction>().map { it.groupId })
        dragStartPosition?.let { startPosition ->
            val title = (items[position] as? MultiSelectAction)?.analyticsValue ?: AnalyticsTracker.INVALID_OR_NULL_VALUE
            val movedFrom = sectionAt(startPosition)
            val movedTo = sectionAt(position)
            val newPosition = if (movedTo == ShelfActionSourceType.Shelf) {
                position - 1
            } else {
                position - (items.indexOf(maxToolbarIcons) + 1)
            }
            eventHorizon.track(
                MultiSelectViewOverflowMenuRearrangeActionMovedEvent(
                    source = source.analyticsValue,
                    title = title,
                    position = newPosition.toLong(),
                    movedFrom = movedFrom,
                    movedTo = movedTo,
                ),
            )
            dragStartPosition = null
        }
    }

    fun onFinished() {
        eventHorizon.track(MultiSelectViewOverflowMenuRearrangeFinishedEvent(source = source.analyticsValue))
    }

    private fun sectionAt(position: Int) = if (position <= maxToolbarIcons) {
        ShelfActionSourceType.Shelf
    } else {
        ShelfActionSourceType.OverflowMenu
    }
}
