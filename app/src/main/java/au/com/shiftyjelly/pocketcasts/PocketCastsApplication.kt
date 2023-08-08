package au.com.shiftyjelly.pocketcasts

import android.app.Application
import android.os.Environment
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.AnonymousBumpStatsTracker
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.TracksAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageOptions
import au.com.shiftyjelly.pocketcasts.repositories.jobs.VersionMigrationsJob
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.shared.AppLifecycleObserver
import au.com.shiftyjelly.pocketcasts.ui.helper.AppIcon
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.TimberDebugTree
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.utils.log.LogBufferUncaughtExceptionHandler
import au.com.shiftyjelly.pocketcasts.utils.log.RxJavaUncaughtExceptionHandling
import coil.Coil
import coil.ImageLoader
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.HiltAndroidApp
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltAndroidApp
class PocketCastsApplication : Application(), Configuration.Provider {

    @Inject lateinit var appLifecycleObserver: AppLifecycleObserver
    @Inject lateinit var statsManager: StatsManager
    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var episodeManager: EpisodeManager
    @Inject lateinit var settings: Settings
    @Inject lateinit var fileStorage: FileStorage
    @Inject lateinit var playlistManager: PlaylistManager
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var downloadManager: DownloadManager
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var subscriptionManager: SubscriptionManager
    @Inject lateinit var userEpisodeManager: UserEpisodeManager
    @Inject lateinit var appIcon: AppIcon
    @Inject lateinit var coilImageLoader: ImageLoader
    @Inject lateinit var userManager: UserManager
    @Inject lateinit var tracksTracker: TracksAnalyticsTracker
    @Inject lateinit var bumpStatsTracker: AnonymousBumpStatsTracker
    @Inject lateinit var syncManager: SyncManager

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    // .penaltyDeath()
                    .build()
            )
        }

        super.onCreate()

        RxJavaUncaughtExceptionHandling.setUp()
        setupSentry()
        setupLogging()
        setupAnalytics()
        setupApp()
    }

    private fun setupAnalytics() {
        AnalyticsTracker.register(tracksTracker, bumpStatsTracker)
        AnalyticsTracker.init(settings)
        AnalyticsTracker.refreshMetadata()
    }

    private fun setupSentry() {
        Thread.getDefaultUncaughtExceptionHandler()?.let {
            Thread.setDefaultUncaughtExceptionHandler(LogBufferUncaughtExceptionHandler(it))
        }

        SentryAndroid.init(this) { options ->
            options.dsn = if (settings.sendCrashReports.flow.value) settings.getSentryDsn() else ""
            options.setTag(SentryHelper.GLOBAL_TAG_APP_PLATFORM, AppPlatform.MOBILE.value)
        }

        // Link email to Sentry crash reports only if the user has opted in
        if (settings.linkCrashReportsToUser.flow.value) {
            syncManager.getEmail()?.let { syncEmail ->
                val user = User().apply { email = syncEmail }
                Sentry.setUser(user)
            }
        }

        // Setup the Firebase, the documentation says this isn't needed but in production we sometimes get the following error "FirebaseApp is not initialized in this process au.com.shiftyjelly.pocketcasts. Make sure to call FirebaseApp.initializeApp(Context) first."
        FirebaseApp.initializeApp(this)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setExecutor(Executors.newFixedThreadPool(3))
            .setJobSchedulerJobIdRange(1000, 20000)
            .build()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun setupApp() {
        LogBuffer.i("Application", "App started. ${settings.getVersion()} (${settings.getVersionCode()})")

        runBlocking {
            appIcon.enableSelectedAlias(appIcon.activeAppIcon)

            FirebaseAnalyticsTracker.setup(
                analytics = FirebaseAnalytics.getInstance(this@PocketCastsApplication),
                settings = settings
            )
            notificationHelper.setupNotificationChannels()
            appLifecycleObserver.setup()

            Coil.setImageLoader(coilImageLoader)

            withContext(Dispatchers.Default) {
                playbackManager.setup()
                downloadManager.setup(episodeManager, podcastManager, playlistManager, playbackManager)

                val isRestoreFromBackup = settings.isRestoreFromBackup()
                // as this may be a different device clear the storage location on a restore
                if (isRestoreFromBackup) {
                    settings.setStorageChoice(null, null)
                }

                // migrate old storage locations
                val storageChoice = settings.getStorageChoice()
                if (storageChoice == null) {
                    // the user doesn't have a storage choice, give them one
                    val storageOptions = StorageOptions()
                    val locationsAvailable = storageOptions.getFolderLocations(this@PocketCastsApplication)
                    if (locationsAvailable.size > 0) {
                        val folder = locationsAvailable[0]
                        settings.setStorageChoice(folder.filePath, folder.label)
                    } else {
                        val location = this@PocketCastsApplication.filesDir
                        settings.setStorageCustomFolder(location.absolutePath)
                    }
                } else if (storageChoice.equals(Settings.LEGACY_STORAGE_ON_PHONE, ignoreCase = true)) {
                    val location = this@PocketCastsApplication.filesDir
                    settings.setStorageCustomFolder(location.absolutePath)
                } else if (storageChoice.equals(Settings.LEGACY_STORAGE_ON_SD_CARD, ignoreCase = true)) {
                    val location = findExternalStorageDirectory()
                    settings.setStorageCustomFolder(location.absolutePath)
                }

                // after the app is installed check it
                if (isRestoreFromBackup) {
                    val podcasts = podcastManager.findSubscribed()
                    val restoredFromBackup = podcasts.isNotEmpty()
                    if (restoredFromBackup) {
                        // check to see if the episode files already exist
                        episodeManager.updateAllEpisodeStatus(EpisodeStatusEnum.NOT_DOWNLOADED)
                        fileStorage.fixBrokenFiles(episodeManager)
                        // reset stats
                        statsManager.reset()
                    }
                    settings.setRestoreFromBackupEnded()
                }

                // create opml import folder
                try {
                    fileStorage.opmlFileFolder
                } catch (e: Exception) {
                    Timber.e(e, "Unable to create opml folder.")
                }

                VersionMigrationsJob.run(
                    podcastManager = podcastManager,
                    settings = settings,
                    syncManager = syncManager,
                    context = this@PocketCastsApplication
                )

                // check that we have .nomedia files in existing folders
                fileStorage.checkNoMediaDirs()

                // init the stats engine
                statsManager.initStatsEngine()

                subscriptionManager.connectToGooglePlay(this@PocketCastsApplication)
            }
        }

        GlobalScope.launch(Dispatchers.IO) { fileStorage.fixBrokenFiles(episodeManager) }

        userEpisodeManager.monitorUploads(applicationContext)
        downloadManager.beginMonitoringWorkManager(applicationContext)
        userManager.beginMonitoringAccountManager(playbackManager)

        Timber.i("Launched ${BuildConfig.APPLICATION_ID}")
    }

    @Suppress("DEPRECATION")
    private fun findExternalStorageDirectory(): File {
        return Environment.getExternalStorageDirectory()
    }

    override fun onTerminate() {
        super.onTerminate()
        LogBuffer.i("Application", "Application terminating")
    }

    private fun setupLogging() {
        LogBuffer.setup(File(filesDir, "logs").absolutePath)
        if (BuildConfig.DEBUG) {
            Timber.plant(TimberDebugTree())
        }
    }
}
