package au.com.shiftyjelly.pocketcasts.voicecontrol.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class EarconPlayer(context: Context) {
    private val soundPool: SoundPool
    private val idToSoundId: Map<EarconId, Int>
    private var released = false

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attrs)
            .build()

        idToSoundId = EarconId.entries.associateWith { id ->
            val resId = context.resources.getIdentifier(
                "earcon_${id.name.lowercase()}",
                "raw",
                context.packageName,
            )
            if (resId != 0) soundPool.load(context, resId, 1) else 0
        }
    }

    fun play(id: EarconId): Boolean {
        if (released) return false
        val soundId = idToSoundId[id] ?: return false
        if (soundId == 0) return false
        soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
        return true
    }

    fun release() {
        released = true
        soundPool.release()
    }
}
