package au.com.shiftyjelly.pocketcasts.preferences

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.core.net.toUri

class NotificationSound(
    val path: String = DEFAULT_SOUND,
    context: Context,
) {
    val uri: Uri? = run {
        val isSoundOn = AudioManager.RINGER_MODE_NORMAL == (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.ringerMode
        if (path.isEmpty() || !isSoundOn) null else path.toUri()
    }

    companion object {
        const val DEFAULT_SOUND = "DEFAULT_SOUND"
    }
}
