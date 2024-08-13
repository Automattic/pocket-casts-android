package au.com.shiftyjelly.pocketcasts

import android.annotation.SuppressLint
import android.app.Application
import android.app.UiModeManager
import android.util.Log
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.crashlogging.InitializeRemoteLogging
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.TimberDebugTree
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.utils.log.RxJavaUncaughtExceptionHandling
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

@SuppressLint("LogNotTimber")
@HiltAndroidApp
class AutomotiveApplication : Application(), Configuration.Provider {

    @Inject lateinit var podcastManager: PodcastManager

    @Inject lateinit var episodeManager: EpisodeManager

    @Inject lateinit var playlistManager: PlaylistManager

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var downloadManager: DownloadManager

    @Inject lateinit var userEpisodeManager: UserEpisodeManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var userManager: UserManager

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var initializeRemoteLogging: InitializeRemoteLogging

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        RxJavaUncaughtExceptionHandling.setUp()
        setupRemoteLogging()
        setupLogging()
        setupAnalytics()
        setupApp()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setExecutor(Executors.newFixedThreadPool(3))
            .setJobSchedulerJobIdRange(1000, 20000)
            .build()
    }

    private fun setupApp() {
        Log.i(Settings.LOG_TAG_AUTO, "App started. ${settings.getVersion()} (${settings.getVersionCode()})")

        runBlocking {
            withContext(Dispatchers.Default) {
                playbackManager.setup()
                downloadManager.setup(episodeManager, podcastManager, playlistManager, playbackManager)
                RefreshPodcastsTask.runNow(this@AutomotiveApplication, applicationScope)
            }
        }

        val playServices = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
        Log.i(Settings.LOG_TAG_AUTO, "Play services $playServices")

        userEpisodeManager.monitorUploads(applicationContext)
        downloadManager.beginMonitoringWorkManager(applicationContext)
        userManager.beginMonitoringAccountManager(playbackManager)

        // force the Automotive app into car mode as some car companies send the UI mode as normal, this makes sure the car resources such as layout-car are used.
        this.getSystemService<UiModeManager>()?.enableCarMode(0)
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(Settings.LOG_TAG_AUTO, "Terminate")
    }

    private fun setupRemoteLogging() {
        initializeRemoteLogging()
    }

    private fun setupLogging() {
        LogBuffer.setup(File(filesDir, "logs").absolutePath)
        if (BuildConfig.DEBUG) {
            Timber.plant(TimberDebugTree())
        }
    }

    private fun setupAnalytics() {
        analyticsTracker.clearAllData()
    }
}
