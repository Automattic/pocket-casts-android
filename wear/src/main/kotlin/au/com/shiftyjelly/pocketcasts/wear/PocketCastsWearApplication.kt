package au.com.shiftyjelly.pocketcasts.wear

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import au.com.shiftyjelly.pocketcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsController
import au.com.shiftyjelly.pocketcasts.analytics.experiments.ExperimentProvider
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.crashlogging.InitializeRemoteLogging
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadStatusObserver
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageOptions
import au.com.shiftyjelly.pocketcasts.repositories.jobs.VersionMigrationsWorker
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackServiceToggle
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.stats.PlaybackStatsSyncWorker
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.shared.AppLifecycleObserver
import au.com.shiftyjelly.pocketcasts.shared.DownloadStatisticsReporter
import au.com.shiftyjelly.pocketcasts.utils.TimberDebugTree
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.utils.log.RxJavaUncaughtExceptionHandling
import au.com.shiftyjelly.pocketcasts.wear.networking.ConnectivityLogger
import com.google.firebase.FirebaseApp
import com.squareup.moshi.Moshi
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class PocketCastsWearApplication :
    Application(),
    Configuration.Provider {

    @Inject lateinit var moshi: Moshi

    @Inject lateinit var appLifecycleObserver: AppLifecycleObserver

    @Inject lateinit var downloadStatusObserver: DownloadStatusObserver

    @Inject lateinit var episodeManager: EpisodeManager

    @Inject lateinit var notificationHelper: NotificationHelper

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var podcastManager: PodcastManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var userManager: UserManager

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var analyticsController: AnalyticsController

    @Inject lateinit var experimentProvider: ExperimentProvider

    @Inject lateinit var downloadStatisticsReporter: DownloadStatisticsReporter

    @Inject lateinit var initializeRemoteLogging: InitializeRemoteLogging

    @Inject lateinit var connectivityLogger: ConnectivityLogger

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

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
        connectivityLogger.startMonitoring()
    }

    private fun setupApp() {
        val application = this

        notificationHelper.setupNotificationChannels()
        appLifecycleObserver.setup()
        PlaybackServiceToggle.ensureCorrectServiceEnabled(application)

        // Apply migrations before the UI starts, so the app never reads pre-migration state.
        VersionMigrationsWorker.performMigrations(
            context = application,
            settings = settings,
            moshi = moshi,
        )

        // Defer the expensive playback/storage setup and monitors off the main thread to avoid blocking onCreate (ANRs).
        applicationScope.launch {
            playbackManager.setup()
            val storageChoice = settings.getStorageChoice()
            if (storageChoice == null) {
                val folder = StorageOptions()
                    .getFolderLocations(application)
                    .firstOrNull()
                if (folder != null) {
                    settings.setStorageChoice(folder.filePath, folder.label)
                } else {
                    settings.setStorageCustomFolder(application.filesDir.absolutePath)
                }
            }

            userManager.beginMonitoringAccountManager(playbackManager)
            downloadStatusObserver.monitorDownloadStatus()

            PlaybackStatsSyncWorker.scheduleOneTimeWork(application)
            PlaybackStatsSyncWorker.schedulePeriodicWork(application)
        }
    }

    private fun setupAnalytics() {
        analyticsController.clearAllData()
        analyticsController.refreshMetadata()
        experimentProvider.initialize()
        downloadStatisticsReporter.setup()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setExecutor(Executors.newFixedThreadPool(3))
            .setJobSchedulerJobIdRange(1000, 20000)
            .build()
}
