package au.com.shiftyjelly.pocketcasts.repositories.playback

import kotlin.math.exp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerVolumeFadeOut(
    private var player: Player,
    private val scope: CoroutineScope,
) {
    companion object {
        private const val VOLUME_CHANGES_PER_SECOND = 30.0
        private const val FADE_VELOCITY = 2.0
        private const val FROM_VOLUME = 1.0
        private const val TO_VOLUME = 0.0
    }

    fun performFadeOut(duration: Double, onStopPlaying: () -> Unit) {
        val totalSteps = (duration * VOLUME_CHANGES_PER_SECOND).toInt()
        val delayBetweenSteps = (1000 / VOLUME_CHANGES_PER_SECOND).toLong()

        var currentStep = 0

        scope.launch(Dispatchers.Main) {
            while (currentStep < totalSteps) {
                val normalizedTime = (currentStep.toDouble() / totalSteps).coerceIn(0.0, 1.0)
                val volumeMultiplier = exp(-FADE_VELOCITY * normalizedTime) * (1 - normalizedTime)
                val newVolume = TO_VOLUME + (FROM_VOLUME - TO_VOLUME) * volumeMultiplier

                if (player.isPlaying()) {
                    player.setVolume(newVolume.toFloat())
                } else {
                    onStopPlaying()
                    break
                }

                currentStep++
                delay(delayBetweenSteps)
            }

            player.setVolume(TO_VOLUME.toFloat())
        }
    }
}
