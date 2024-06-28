package au.com.shiftyjelly.pocketcasts.wear

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import au.com.shiftyjelly.pocketcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.crashlogging.InitializeRemoteLogging
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageOptions
import au.com.shiftyjelly.pocketcasts.repositories.jobs.VersionMigrationsJob
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.shared.AppLifecycleObserver
import au.com.shiftyjelly.pocketcasts.shared.DownloadStatisticsReporter
import au.com.shiftyjelly.pocketcasts.utils.TimberDebugTree
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.utils.log.RxJavaUncaughtExceptionHandling
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltAndroidApp
class PocketCastsWearApplication : Application(), Configuration.Provider {

    @Inject lateinit var appLifecycleObserver: AppLifecycleObserver

    @Inject lateinit var downloadManager: DownloadManager

    @Inject lateinit var episodeManager: EpisodeManager

    @Inject lateinit var notificationHelper: NotificationHelper

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var playlistManager: PlaylistManager

    @Inject lateinit var podcastManager: PodcastManager

    @Inject lateinit var syncManager: SyncManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var userManager: UserManager

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    @Inject lateinit var downloadStatisticsReporter: DownloadStatisticsReporter

    @Inject lateinit var initializeRemoteLogging: InitializeRemoteLogging

    override fun onCreate() {
        super.onCreate()
        RxJavaUncaughtExceptionHandling.setUp()
        setupCrashLogging()
        setupLogging()
        setupAnalytics()
        setupApp()
    }

    private fun setupCrashLogging() {
        initializeRemoteLogging()
        // Setup the Firebase, the documentation says this isn't needed but in production we sometimes get the following error "FirebaseApp is not initialized in this process au.com.shiftyjelly.pocketcasts. Make sure to call FirebaseApp.initializeApp(Context) first."
        FirebaseApp.initializeApp(this)
    }

    private fun setupLogging() {
        LogBuffer.setup(File(filesDir, "logs").absolutePath)
        if (BuildConfig.DEBUG) {
            Timber.plant(TimberDebugTree())
        }
    }

    private fun setupApp() {
        runBlocking {
            FirebaseAnalyticsTracker.setup(
                analytics = FirebaseAnalytics.getInstance(this@PocketCastsWearApplication),
                settings = settings,
            )

            notificationHelper.setupNotificationChannels()
            appLifecycleObserver.setup()

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

            VersionMigrationsJob.run(
                podcastManager = podcastManager,
                settings = settings,
                syncManager = syncManager,
                context = this@PocketCastsWearApplication,
            )
        }

        userManager.beginMonitoringAccountManager(playbackManager)
        downloadManager.beginMonitoringWorkManager(applicationContext)
    }

    private fun setupAnalytics() {
        analyticsTracker.clearAllData()
        analyticsTracker.refreshMetadata()
        downloadStatisticsReporter.setup()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setExecutor(Executors.newFixedThreadPool(3))
            .setJobSchedulerJobIdRange(1000, 20000)
            .build()
    }
}
