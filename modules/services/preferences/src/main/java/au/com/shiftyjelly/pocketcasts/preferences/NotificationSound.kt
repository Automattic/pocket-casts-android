package au.com.shiftyjelly.pocketcasts.preferences

import android.content.Context
import android.media.AudioManager
import android.net.Uri

class NotificationSound(
    val path: String = defaultPath,
    context: Context,
) {
    val uri: Uri? = run {
        val isSoundOn =
            AudioManager.RINGER_MODE_NORMAL ==
                (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.ringerMode
        if (path.isEmpty() || !isSoundOn) null else Uri.parse(path)
    }

    companion object {
        const val defaultPath = "DEFAULT_SOUND"
    }
}
