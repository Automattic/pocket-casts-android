package au.com.shiftyjelly.pocketcasts.wear.data.service.playback

import androidx.media3.session.MediaSession
import com.google.android.horologist.media3.service.LifecycleMediaLibraryService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : LifecycleMediaLibraryService() {
    @Inject
    public override lateinit var mediaLibrarySession: MediaLibrarySession

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaLibrarySession
    }
}
