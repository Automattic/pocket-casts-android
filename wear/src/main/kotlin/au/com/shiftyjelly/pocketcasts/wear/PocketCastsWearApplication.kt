package au.com.shiftyjelly.pocketcasts.wear

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import au.com.shiftyjelly.pocketcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.experiments.ExperimentProvider
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.coroutines.di.MainDispatcher
import au.com.shiftyjelly.pocketcasts.coroutines.di.WearDefaultDispatcher
import au.com.shiftyjelly.pocketcasts.crashlogging.InitializeRemoteLogging
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageOptions
import au.com.shiftyjelly.pocketcasts.repositories.jobs.VersionMigrationsWorker
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltAndroidApp
class PocketCastsWearApplication :
    Application(),
    Configuration.Provider {

    @Inject lateinit var moshi: dagger.Lazy<Moshi>

    @Inject lateinit var appLifecycleObserver: AppLifecycleObserver

    @Inject lateinit var downloadManager: DownloadManager

    @Inject lateinit var episodeManager: dagger.Lazy<EpisodeManager>

    @Inject lateinit var notificationHelper: NotificationHelper

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var podcastManager: dagger.Lazy<PodcastManager>

    @Inject lateinit var settings: Settings

    @Inject lateinit var userManager: UserManager

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var analyticsTracker: dagger.Lazy<AnalyticsTracker>

    @Inject lateinit var experimentProvider: dagger.Lazy<ExperimentProvider>

    @Inject lateinit var downloadStatisticsReporter: dagger.Lazy<DownloadStatisticsReporter>

    @Inject lateinit var initializeRemoteLogging: InitializeRemoteLogging

    @Inject lateinit var connectivityLogger: ConnectivityLogger

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject @WearDefaultDispatcher
    lateinit var defaultDispatcher: CoroutineDispatcher

    @Inject @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    override fun onCreate() {
        super.onCreate()
        RxJavaUncaughtExceptionHandling.setUp()
        setupCrashLogging()
        setupLogging()

        // Launch initialization asynchronously with parallel optimization.
        // Parallel execution of independent operations reduces total initialization time.
        applicationScope.launch {
            setupApp()
            setupAnalytics()
        }
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

    private suspend fun setupApp() {
        // Parallelize independent initialization to reduce total time.
        // Group operations by thread requirement to minimize context switches.
        coroutineScope {
            // Parallel track 1: Main thread operations
            val mainThreadJob = launch(mainDispatcher) {
                notificationHelper.setupNotificationChannels()
                appLifecycleObserver.setup()
            }

            // Parallel track 2: Background operations
            val backgroundJob = launch(defaultDispatcher) {
                playbackManager.setup()
                downloadManager.setup(episodeManager.get(), podcastManager.get(), playbackManager)

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

            // Wait for parallel operations to complete
            mainThreadJob.join()
            backgroundJob.join()
        }

        // Sequential operations that depend on previous setup
        VersionMigrationsWorker.performMigrations(
            context = this@PocketCastsWearApplication,
            settings = settings,
            moshi = moshi.get(),
        )

        // Final main thread setup
        withContext(mainDispatcher) {
            userManager.beginMonitoringAccountManager(playbackManager)
        }

        downloadManager.beginMonitoringWorkManager(applicationContext)
    }

    private suspend fun setupAnalytics() {
        // Analytics operations can run in parallel with main thread reporter setup
        coroutineScope {
            val analyticsJob = launch(defaultDispatcher) {
                analyticsTracker.get().clearAllData()
                analyticsTracker.get().refreshMetadata()
                experimentProvider.get().initialize()
            }

            val reporterJob = launch(mainDispatcher) {
                downloadStatisticsReporter.get().setup()
            }

            analyticsJob.join()
            reporterJob.join()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setExecutor(Executors.newFixedThreadPool(3))
            .setJobSchedulerJobIdRange(1000, 20000)
            .build()
}
