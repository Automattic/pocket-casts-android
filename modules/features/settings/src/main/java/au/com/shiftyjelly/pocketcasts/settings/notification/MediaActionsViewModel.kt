package au.com.shiftyjelly.pocketcasts.settings.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.rearrange.MenuAction
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
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
    val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    data class State(
        val customActionsVisibility: Boolean = false,
        val actions: List<MenuAction> = emptyList(),
    )

    private val mutableState = MutableStateFlow(State())
    val state: StateFlow<State> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settings.mediaControlItems.flow,
                settings.customMediaActionsVisibility.flow,
            ) { items, visibility ->
                State(
                    customActionsVisibility = visibility,
                    actions = items.map { MenuAction(key = it.key, name = it.controlName, icon = it.iconRes) },
                )
            }
                .catch { e -> Timber.e(e, "Error reading settings") }
                .collect { mutableState.value = it }
        }
    }

    fun setShowCustomActionsChanged(visibility: Boolean) {
        settings.customMediaActionsVisibility.set(visibility, updateModifiedAt = true)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_GENERAL_MEDIA_NOTIFICATION_CONTROLS_SHOW_CUSTOM_TOGGLED,
            mapOf("enabled" to visibility),
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

        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_GENERAL_MEDIA_NOTIFICATION_CONTROLS_ORDER_CHANGED,
            mapOf(
                "item" to (MediaNotificationControls.itemForId(action.key)?.serverId ?: ""),
                "previous_position" to fromIndex,
                "updated_position" to toIndex,
            ),
        )
    }
}
