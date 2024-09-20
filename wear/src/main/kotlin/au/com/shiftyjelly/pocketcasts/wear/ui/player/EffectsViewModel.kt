package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffectsData
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.roundedSpeed
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class EffectsViewModel
@Inject constructor(
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
) : ViewModel() {

    val state: StateFlow<State> =
        settings.globalPlaybackEffects.flow
            .map { State.Loaded(it.toData()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = State.Loading,
            )

    fun increasePlaybackSpeed() {
        val currentState = state.value as? State.Loaded ?: return
        changePlaybackSpeed(currentState.playbackEffects.playbackSpeed + 0.1)
    }

    fun decreasePlaybackSpeed() {
        val currentState = state.value as? State.Loaded ?: return
        changePlaybackSpeed(currentState.playbackEffects.playbackSpeed - 0.1)
    }

    private fun changePlaybackSpeed(speed: Double) {
        val currentState = state.value as? State.Loaded ?: return
        val effects = currentState.playbackEffects
        val newEffects = effects.copy(playbackSpeed = speed.roundedSpeed())
        saveEffects(newEffects)
    }

    fun updateTrimSilence(trimMode: TrimMode) {
        val currentState = state.value as? State.Loaded ?: return
        val effects = currentState.playbackEffects
        val newEffects = effects.copy(trimMode = trimMode)
        saveEffects(newEffects)
    }

    fun updateBoostVolume(boostVolume: Boolean) {
        val currentState = state.value as? State.Loaded ?: return
        val effects = currentState.playbackEffects
        val newEffects = effects.copy(isVolumeBoosted = boostVolume)
        saveEffects(newEffects)
    }

    private fun saveEffects(effectsData: PlaybackEffectsData) {
        val effects = effectsData.toEffects()
        viewModelScope.launch {
            playbackManager.updatePlayerEffects(effects)
            settings.globalPlaybackEffects.set(effects, updateModifiedAt = true)
        }
    }

    sealed class State {
        data class Loaded(
            val playbackEffects: PlaybackEffectsData,
        ) : State()

        object Loading : State()
    }
}
