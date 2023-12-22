package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import java.util.Locale

class PlaybackEffects {
    @Volatile var playbackSpeed = 1.0
    @Volatile var trimMode: TrimMode = TrimMode.OFF
    @Volatile var isVolumeBoosted = false

    val usingDefaultValues: Boolean
        get() = !isVolumeBoosted && trimMode == TrimMode.OFF && playbackSpeed == 1.0

    override fun toString(): String {
        return String.format(Locale.ENGLISH, "Speed: %f Trim: %d Boost: %s", playbackSpeed, trimMode.ordinal, isVolumeBoosted)
    }

    fun toData() = PlaybackEffectsData(playbackSpeed, trimMode, isVolumeBoosted)
}

// This class can be useful when you need things like a well-behaved equals()
// method, or a "copy" method.
data class PlaybackEffectsData(
    val playbackSpeed: Double,
    val trimMode: TrimMode,
    val isVolumeBoosted: Boolean,
) {
    fun toEffects() = PlaybackEffects().apply {
        playbackSpeed = this@PlaybackEffectsData.playbackSpeed
        trimMode = this@PlaybackEffectsData.trimMode
        isVolumeBoosted = this@PlaybackEffectsData.isVolumeBoosted
    }
}
