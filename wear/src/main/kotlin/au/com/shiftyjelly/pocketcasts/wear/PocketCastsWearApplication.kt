package au.com.shiftyjelly.pocketcasts.wear

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import au.com.shiftyjelly.pocketcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.AnonymousBumpStatsTracker
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.TracksAnalyticsTracker
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
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.TimberDebugTree
import au.com.shiftyjelly.pocketcasts.utils.log.RxJavaUncaughtExceptionHandling
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.HiltAndroidApp
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject

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

    @Inject lateinit var tracksTracker: TracksAnalyticsTracker
    @Inject lateinit var bumpStatsTracker: AnonymousBumpStatsTracker
    @Inject lateinit var syncManager: SyncManager

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
            options.dsn = if (settings.getSendCrashReports()) settings.getSentryDsn() else ""
            options.setTag(SentryHelper.GLOBAL_TAG_APP_PLATFORM, AppPlatform.WEAR.value)
        }

        // Link email to Sentry crash reports only if the user has opted in
        if (settings.getLinkCrashReportsToUser()) {
            syncManager.getEmail()?.let { syncEmail ->
                val user = User().apply { email = syncEmail }
                Sentry.setUser(user)
            }
        }

        // Setup the Firebase, the documentation says this isn't needed but in production we sometimes get the following error "FirebaseApp is not initialized in this process au.com.shiftyjelly.pocketcasts. Make sure to call FirebaseApp.initializeApp(Context) first."
        FirebaseApp.initializeApp(this)
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
                context = this@PocketCastsWearApplication
            )
        }

        userManager.beginMonitoringAccountManager(playbackManager)
        downloadManager.beginMonitoringWorkManager(applicationContext)
    }

    private fun setupAnalytics() {
        AnalyticsTracker.register(tracksTracker, bumpStatsTracker)
        AnalyticsTracker.init(settings)
        AnalyticsTracker.refreshMetadata()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setExecutor(Executors.newFixedThreadPool(3))
            .setJobSchedulerJobIdRange(1000, 20000)
            .build()
    }
}
