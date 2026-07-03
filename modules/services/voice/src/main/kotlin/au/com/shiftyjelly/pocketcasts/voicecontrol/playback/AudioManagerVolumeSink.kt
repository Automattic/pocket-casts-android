package au.com.shiftyjelly.pocketcasts.voicecontrol.playback

import android.content.Context
import android.media.AudioManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.EarconId
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioManagerVolumeSink @Inject constructor(
    @ApplicationContext private val context: Context,
) : VoiceVolumeSink {
    private val audioManager: AudioManager
        get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun setVolume(volume: Int): VoiceResponse {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val scaled = (volume * max / 100).coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, scaled, 0)
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun adjustVolume(delta: Int): VoiceResponse {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val target = (current + delta * max / 100).coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        return VoiceResponse.Earcon(EarconId.SUCCESS)
    }

    override fun query(): VoiceResponse.Spoken {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val percent = if (max > 0) current * 100 / max else 0
        return VoiceResponse.Spoken("Volume $percent percent")
    }
}
