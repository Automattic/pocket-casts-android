package au.com.shiftyjelly.pocketcasts

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackService
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber

@AndroidEntryPoint
class AutoPlaybackService : PlaybackService() {

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        settings.setAutomotiveConnectedToMediaSession(false)

        RefreshPodcastsTask.runNow(this, applicationScope)

        Timber.d("Auto playback service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("Auto playback service destroyed")

        playbackManager.pause(transientLoss = false, sourceView = SourceView.AUTO_PAUSE)
    }
}
