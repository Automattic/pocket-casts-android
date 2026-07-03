package au.com.shiftyjelly.pocketcasts.voicecontrol.playback

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.EarconId
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class PlaybackManagerEffectsSink @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
) : VoiceEffectsSink {
    override fun setSpeed(speed: Double): VoiceResponse {
        val clamped = speed.coerceIn(0.5, 5.0)
        applySpeed(clamped)
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun adjustSpeed(delta: Double): VoiceResponse {
        val current = playbackManager.getPlaybackSpeed()
        val clamped = (current + delta).coerceIn(0.5, 5.0)
        val rounded = (clamped * 10).roundToInt() / 10.0
        applySpeed(rounded)
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun setTrimMode(mode: String): VoiceResponse {
        val trimMode = au.com.shiftyjelly.pocketcasts.models.type.TrimMode.entries.find {
            it.name.equals(mode, ignoreCase = true)
        } ?: au.com.shiftyjelly.pocketcasts.models.type.TrimMode.OFF
        val effects = settings.globalPlaybackEffects.value
        effects.trimMode = trimMode
        settings.globalPlaybackEffects.set(effects, updateModifiedAt = true)
        playbackManager.updatePlayerEffects(effects = effects)
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun setVolumeBoost(enabled: Boolean): VoiceResponse {
        val effects = settings.globalPlaybackEffects.value
        effects.isVolumeBoosted = enabled
        settings.globalPlaybackEffects.set(effects, updateModifiedAt = true)
        playbackManager.updatePlayerEffects(effects = effects)
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun queryEffects(): VoiceResponse.Spoken {
        val effects = settings.globalPlaybackEffects.value
        val speed = effects.playbackSpeed
        val trim = effects.trimMode.name.lowercase()
        val boost = if (effects.isVolumeBoosted) "on" else "off"
        return VoiceResponse.Spoken("Speed $speed, trim silence $trim, volume boost $boost")
    }

    private fun applySpeed(speed: Double) {
        val effects = settings.globalPlaybackEffects.value
        effects.playbackSpeed = speed
        settings.globalPlaybackEffects.set(effects, updateModifiedAt = true)
        playbackManager.updatePlayerEffects(effects = effects)
    }
}
