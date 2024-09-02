package au.com.shiftyjelly.pocketcasts.settings.notification

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@HiltViewModel
class MediaActionsViewModel @Inject constructor(
    val settings: Settings,
) : ViewModel() {

    val state: Flow<State> = combine(
        settings.mediaControlItems.flow,
        settings.customMediaActionsVisibility.flow,
    ) { items, visibility ->
        State(
            customActionsVisibility = visibility,
            actions = items,
        )
    }

    fun setShowCustomActionsChanged(visibility: Boolean) {
        settings.customMediaActionsVisibility.set(visibility, updateModifiedAt = true)
    }

    fun onActionsOrderChanged(mediaNotificationControls: List<MediaNotificationControls>) {
        settings.mediaControlItems.set(mediaNotificationControls, updateModifiedAt = true)
    }

//    private fun buildActions(items: List<MediaNotificationControls>): List<ListItem> =
//        buildList {
//            addAll(items.map { ListItem.ListItemAction(it) })
//            add(3, ListItem.ListItemTitle(LR.string.settings_other_media_actions))
//        }

//    sealed class ListItem {
//        data class ListItemTitle(@StringRes val id: Int) : ListItem()
//        data class ListItemAction(val item: MediaNotificationControls) : ListItem()
//    }

    data class State(
        val customActionsVisibility: Boolean = false,
        val actions: List<MediaNotificationControls> = emptyList(),
    )
}
