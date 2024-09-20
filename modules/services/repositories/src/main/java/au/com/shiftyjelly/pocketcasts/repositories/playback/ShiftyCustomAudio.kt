package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import timber.log.Timber

class ShiftyCustomAudio(
    private val statsManager: StatsManager,
) {
    private var enhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var boostVolume = false
    var playbackSpeed = 0f

    fun setBoostVolume(boostVolume: Boolean) {
        this.boostVolume = boostVolume
        enhancer?.setEnabled(boostVolume)
        equalizer?.setEnabled(boostVolume)
    }

    fun setupVolumeBoost(audioSessionId: Int) {
        try {
            enhancer = LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(LOUDNESS_TARGET_GAIN)
                enabled = boostVolume
            }
        } catch (e: Exception) {
            // Some devices don't support the loudness enhancer. They'll throw an exception when you try and create one.
            // Samsung S5 and LG G3 both do this. Seems to work on the Nexus devices mainly.
            Timber.e(e, "Failed to create loudness enhancer")
        }

        if (enhancer == null) {
            try {
                equalizer = Equalizer(0, audioSessionId).apply {
                    repeat(numberOfBands.toInt()) { intBand ->
                        val band = intBand.toShort()
                        when (getCenterFreq(band) / 1000) {
                            in 0..<100 -> setBandLevel(band, (-5 * 100).toShort())
                            in 100..<250 -> setBandLevel(band, 0.toShort())
                            in 250..<1000 -> setBandLevel(band, (10 * 100).toShort())
                            in 1000..<2000 -> setBandLevel(band, (12 * 100).toShort())
                            in 2000..<10000 -> setBandLevel(band, (8 * 100).toShort())
                            else -> setBandLevel(band, 0.toShort())
                        }
                    }
                    enabled = boostVolume
                }
            } catch (e: Exception) {
                // Some devices don't support the equalizer. They'll throw an exception when you try and create one.
                Timber.e(e, "Failed to create equalizer")
            }
        }
    }

    fun addSilenceSkippedTime(timeUs: Long) {
        statsManager.addTimeSavedSilenceRemoval(timeUs / 1000)
    }

    fun addVariableSpeedTime(timeUs: Long, speed: Float) {
        statsManager.addTimeSavedVariableSpeed((((timeUs * speed) - timeUs) / 1000).toLong())
    }

    private companion object {
        const val LOUDNESS_TARGET_GAIN = 1000
    }
}
