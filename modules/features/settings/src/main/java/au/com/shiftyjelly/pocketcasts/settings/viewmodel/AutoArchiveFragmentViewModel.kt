package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SettingsAutoArchiveInactiveChangedEvent
import com.automattic.eventhorizon.SettingsAutoArchiveIncludeStarredToggledEvent
import com.automattic.eventhorizon.SettingsAutoArchivePlayedChangedEvent
import com.automattic.eventhorizon.SettingsAutoArchiveShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class AutoArchiveFragmentViewModel @Inject constructor(
    private val settings: Settings,
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    private val mutableState = MutableStateFlow(initState())
    val state: StateFlow<State> = mutableState

    fun trackOnViewShownEvent() {
        eventHorizon.track(SettingsAutoArchiveShownEvent)
    }

    fun onStarredChanged(newValue: Boolean) {
        settings.autoArchiveIncludesStarred.set(newValue, updateModifiedAt = true)
        eventHorizon.track(
            SettingsAutoArchiveIncludeStarredToggledEvent(
                enabled = newValue,
            ),
        )
        mutableState.update { it.copy(starredEpisodes = newValue) }
    }

    fun onPlayedEpisodesAfterChanged(newValue: AutoArchiveAfterPlaying) {
        settings.autoArchiveAfterPlaying.set(newValue, updateModifiedAt = true)
        eventHorizon.track(
            SettingsAutoArchivePlayedChangedEvent(
                value = newValue.eventHorizonValue,
            ),
        )
        mutableState.update { it.copy(archiveAfterPlaying = newValue) }
    }

    fun onInactiveChanged(newValue: AutoArchiveInactive) {
        settings.autoArchiveInactive.set(newValue, updateModifiedAt = true)
        eventHorizon.track(
            SettingsAutoArchiveInactiveChangedEvent(
                value = newValue.eventHorizonValue,
            ),
        )
        mutableState.update { it.copy(archiveInactive = newValue) }
    }

    private fun initState() = State(
        starredEpisodes = settings.autoArchiveIncludesStarred.value,
        archiveAfterPlaying = settings.autoArchiveAfterPlaying.value,
        archiveInactive = settings.autoArchiveInactive.value,
    )

    data class State(
        val starredEpisodes: Boolean,
        val archiveAfterPlaying: AutoArchiveAfterPlaying = AutoArchiveAfterPlaying.Never,
        val archiveInactive: AutoArchiveInactive = AutoArchiveInactive.Never,
    )
}
