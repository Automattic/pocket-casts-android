package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.models.type.TrimMode

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
