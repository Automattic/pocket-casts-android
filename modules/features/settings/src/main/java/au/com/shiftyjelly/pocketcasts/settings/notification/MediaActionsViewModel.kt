package au.com.shiftyjelly.pocketcasts.settings.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.compose.rearrange.MenuAction
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SettingsGeneralMediaNotificationControlsOrderChangedEvent
import com.automattic.eventhorizon.SettingsGeneralMediaNotificationControlsShowCustomToggledEvent
import com.automattic.eventhorizon.SettingsGeneralMediaNotificationNextPreviousTrackSkipButtonsToggledEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class MediaActionsViewModel @Inject constructor(
    val settings: Settings,
    val eventHorizon: EventHorizon,
) : ViewModel() {

    data class State(
        val customActionsVisibility: Boolean = false,
        val nextPreviousTrackSkipButtons: Boolean = false,
        val actions: List<MenuAction> = emptyList(),
    )

    private val mutableState = MutableStateFlow(State())
    val state: StateFlow<State> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settings.mediaControlItems.flow,
                settings.customMediaActionsVisibility.flow,
                settings.nextPreviousTrackSkipButtons.flow,
            ) { items, visibility, skipButtons ->
                State(
                    customActionsVisibility = visibility,
                    nextPreviousTrackSkipButtons = skipButtons,
                    actions = items.map { MenuAction(key = it.key, name = it.controlName, icon = it.iconRes) },
                )
            }
                .catch { e -> Timber.e(e, "Error reading settings") }
                .collect { mutableState.value = it }
        }
    }

    fun setShowCustomActionsChanged(visibility: Boolean) {
        settings.customMediaActionsVisibility.set(visibility, updateModifiedAt = true)
        eventHorizon.track(
            SettingsGeneralMediaNotificationControlsShowCustomToggledEvent(
                enabled = visibility,
            ),
        )
    }

    fun setNextPreviousTrackSkipButtonsChanged(enabled: Boolean) {
        settings.nextPreviousTrackSkipButtons.set(enabled, updateModifiedAt = true)
        eventHorizon.track(
            SettingsGeneralMediaNotificationNextPreviousTrackSkipButtonsToggledEvent(
                enabled = enabled,
            ),
        )
    }

    fun onActionsOrderChanged(menuActions: List<MenuAction>) {
        val mediaNotificationControls = menuActions.mapNotNull { MediaNotificationControls.itemForId(it.key) }
        settings.mediaControlItems.set(mediaNotificationControls, updateModifiedAt = true)
    }

    fun onActionMoved(fromIndex: Int, toIndex: Int, action: MenuAction) {
        if (fromIndex == toIndex) {
            Timber.d("Not tracking move because position did not change")
            return
        }

        eventHorizon.track(
            SettingsGeneralMediaNotificationControlsOrderChangedEvent(
                item = MediaNotificationControls.itemForId(action.key)?.serverId.orEmpty(),
                previousPosition = fromIndex.toLong(),
                updatedPosition = toIndex.toLong(),
            ),
        )
    }
}
