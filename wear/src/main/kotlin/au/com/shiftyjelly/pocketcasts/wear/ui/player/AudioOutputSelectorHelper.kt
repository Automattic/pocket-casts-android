package au.com.shiftyjelly.pocketcasts.wear.ui.player

import au.com.shiftyjelly.pocketcasts.wear.di.IsEmulator
import com.google.android.horologist.audio.AudioOutput
import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.media3.audio.AudioOutputSelector
import javax.inject.Inject
import timber.log.Timber

class AudioOutputSelectorHelper @Inject constructor(
    private val audioRepository: SystemAudioRepository,
    private val audioOutputSelector: AudioOutputSelector,
    @IsEmulator private val isEmulator: Boolean,
) {
    suspend fun attemptPlay(action: () -> Unit) {
        val currentAudioOutput = audioRepository.audioOutput.value

        if (currentAudioOutput.isOutputAllowed) {
            action()
        } else {
            val newAudioOutput = audioOutputSelector.selectNewOutput(currentAudioOutput) ?: AudioOutput.None
            if (newAudioOutput.isOutputAllowed) {
                action()
            } else {
                Timber.e("Cannot play audio on output $newAudioOutput")
            }
        }
    }

    private val AudioOutput.isOutputAllowed get() = isEmulator || this is AudioOutput.BluetoothHeadset
}
