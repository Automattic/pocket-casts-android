package au.com.shiftyjelly.pocketcasts.wear.ui.player

import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.media3.audio.AudioOutputSelector
import com.google.android.horologist.media3.rules.PlaybackRules
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioOutputSelectorHelper @Inject constructor(
    private val audioOutputRepository: SystemAudioRepository,
    private val audioOutputSelector: AudioOutputSelector,
    private val playbackRules: PlaybackRules,
) {
    suspend fun attemptPlay(play: () -> Unit) {
        val currentAudioOutput = audioOutputRepository.audioOutput.value

        val canPlayWithCurrentOutput = playbackRules.canPlayWithOutput(currentAudioOutput)

        if (canPlayWithCurrentOutput) {
            play()
        } else {
            val newAudioOutput = audioOutputSelector.selectNewOutput(currentAudioOutput)

            val canPlayWithNewOutput =
                newAudioOutput != null && playbackRules.canPlayWithOutput(newAudioOutput)

            if (canPlayWithNewOutput) {
                play()
            } else {
                Timber.e("Cannot play audio on output $newAudioOutput")
            }
        }
    }
}
