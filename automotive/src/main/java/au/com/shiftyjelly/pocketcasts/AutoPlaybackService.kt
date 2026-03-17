package au.com.shiftyjelly.pocketcasts

import android.annotation.SuppressLint
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackService
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.MediaItemCompatConverter
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("LogNotTimber")
@AndroidEntryPoint
class AutoPlaybackService : PlaybackService() {

    @Inject lateinit var podcastManager: PodcastManager

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        settings.setAutomotiveConnectedToMediaSession(false)

        RefreshPodcastsTask.runNow(this, applicationScope)

        Log.d(Settings.LOG_TAG_AUTO, "Auto playback service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(Settings.LOG_TAG_AUTO, "Auto playback service destroyed")

        playbackManager.pause(transientLoss = false, sourceView = SourceView.AUTO_PAUSE)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.detach()
        Log.d(Settings.LOG_TAG_AUTO, "onLoadChildren. Loading section $parentId")
        launch(Dispatchers.IO) {
            Log.d(Settings.LOG_TAG_AUTO, "onLoadChildren. Running in background $parentId")
            try {
                val items = browseTreeProvider.loadChildren(parentId, this@AutoPlaybackService)
                Log.d(Settings.LOG_TAG_AUTO, "onLoadChildren. Sending results $parentId")
                result.sendResult(MediaItemCompatConverter.toCompatList(items))
                Log.d(Settings.LOG_TAG_AUTO, "onLoadChildren. Results sent $parentId")
            } catch (e: Exception) {
                Log.e(Settings.LOG_TAG_AUTO, "onLoadChildren. Could not load $parentId", e)
                result.sendResult(emptyList())
            }
            podcastManager.refreshPodcastsIfRequired("Automotive")
        }
    }
}
