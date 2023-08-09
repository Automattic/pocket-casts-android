package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.clipToRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject
import kotlin.math.round

@HiltViewModel
class EffectsViewModel
@Inject constructor(
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
) : ViewModel() {

    val state: StateFlow<State> =
        playbackManager.playbackStateRelay
            .asFlow()
            .map {
                State.Loaded(settings.globalPlaybackEffects.flow.value)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = State.Loading
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
        val clippedToRangeSpeed = speed.clipToRange(0.5, 3.0)
        // to stop the issue 1.2000000000000002
        val roundedSpeed = round(clippedToRangeSpeed * 10.0) / 10.0
        effects.playbackSpeed = roundedSpeed
        saveEffects(effects)
    }

    fun updateTrimSilence(trimMode: TrimMode) {
        val currentState = state.value as? State.Loaded ?: return
        val effects = currentState.playbackEffects
        effects.trimMode = trimMode
        saveEffects(effects)
    }

    fun updateBoostVolume(boostVolume: Boolean) {
        val currentState = state.value as? State.Loaded ?: return
        val effects = currentState.playbackEffects
        effects.isVolumeBoosted = boostVolume
        saveEffects(effects)
    }

    private fun saveEffects(effects: PlaybackEffects) {
        viewModelScope.launch {
            playbackManager.updatePlayerEffects(effects)
            settings.globalPlaybackEffects.set(effects)
        }
    }

    sealed class State {
        data class Loaded(
            val playbackEffects: PlaybackEffects,
        ) : State()

        object Loading : State()
    }
}
