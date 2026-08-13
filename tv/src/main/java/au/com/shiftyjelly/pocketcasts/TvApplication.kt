package au.com.shiftyjelly.pocketcasts

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackServiceToggle
import au.com.shiftyjelly.pocketcasts.utils.TimberDebugTree
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class TvApplication :
    Application(),
    Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var notificationHelper: NotificationHelper

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(TimberDebugTree())
        }
        notificationHelper.setupNotificationChannels()
        PlaybackServiceToggle.ensureCorrectServiceEnabled(this)
        // setup() subscribes the Up Next queue's sync pipeline itself, so there must be no
        // separate UpNextQueue.setupBlocking() call on TV.
        applicationScope.launch {
            playbackManager.setup()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
