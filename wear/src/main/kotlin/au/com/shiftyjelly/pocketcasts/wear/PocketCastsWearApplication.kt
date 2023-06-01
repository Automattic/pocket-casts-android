package au.com.shiftyjelly.pocketcasts.wear

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import au.com.shiftyjelly.pocketcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageOptions
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.TimberDebugTree
import au.com.shiftyjelly.pocketcasts.utils.log.RxJavaUncaughtExceptionHandling
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltAndroidApp
class PocketCastsWearApplication : Application(), Configuration.Provider {

    @Inject lateinit var downloadManager: DownloadManager
    @Inject lateinit var episodeManager: EpisodeManager
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var playlistManager: PlaylistManager
    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var settings: Settings
    @Inject lateinit var userManager: UserManager
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        setupSentry()
        setupLogging()
        setupAnalytics()
        setupApp()

        RxJavaUncaughtExceptionHandling.setUp()
    }

    private fun setupSentry() {
        SentryAndroid.init(this) { options ->
            options.dsn = settings.getSentryDsn()
        }
    }

    private fun setupLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(TimberDebugTree())
        }
    }

    private fun setupApp() {
        runBlocking {
            FirebaseAnalyticsTracker.setup(
                analytics = FirebaseAnalytics.getInstance(this@PocketCastsWearApplication),
                settings = settings
            )

            notificationHelper.setupNotificationChannels()

            withContext(Dispatchers.Default) {
                playbackManager.setup()
                downloadManager.setup(episodeManager, podcastManager, playlistManager, playbackManager)

                val storageChoice = settings.getStorageChoice()
                if (storageChoice == null) {
                    val folder = StorageOptions()
                        .getFolderLocations(this@PocketCastsWearApplication)
                        .firstOrNull()
                    if (folder != null) {
                        settings.setStorageChoice(folder.filePath, folder.label)
                    } else {
                        settings.setStorageCustomFolder(this@PocketCastsWearApplication.filesDir.absolutePath)
                    }
                }
            }
        }

        userManager.beginMonitoringAccountManager(playbackManager)
        downloadManager.beginMonitoringWorkManager(applicationContext)
    }

    private fun setupAnalytics() {
        AnalyticsTracker.init(settings)
        FirebaseApp.initializeApp(this)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setExecutor(Executors.newFixedThreadPool(3))
            .setJobSchedulerJobIdRange(1000, 20000)
            .build()
    }
}
