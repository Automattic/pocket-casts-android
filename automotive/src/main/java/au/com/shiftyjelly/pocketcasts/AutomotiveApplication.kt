package au.com.shiftyjelly.pocketcasts

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import au.com.shiftyjelly.pocketcasts.account.AccountAuth
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.TimberDebugTree
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject

@SuppressLint("LogNotTimber")
@HiltAndroidApp
class AutomotiveApplication : Application(), Configuration.Provider {

    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var episodeManager: EpisodeManager
    @Inject lateinit var playlistManager: PlaylistManager
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var downloadManager: DownloadManager
    @Inject lateinit var userEpisodeManager: UserEpisodeManager
    @Inject lateinit var accountAuth: AccountAuth
    @Inject lateinit var settings: Settings
    @Inject lateinit var userManager: UserManager
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        setupSentry()
        setupLogging()
        setupAnalytics()
        setupAutomotiveDefaults()
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
            FirebaseAnalyticsTracker.setup(FirebaseAnalytics.getInstance(this@AutomotiveApplication), settings)

            withContext(Dispatchers.Default) {
                playbackManager.setup()
                downloadManager.setup(episodeManager, podcastManager, playlistManager, playbackManager)
                RefreshPodcastsTask.runNow(this@AutomotiveApplication)
            }
        }

        val playServices = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
        Log.i(Settings.LOG_TAG_AUTO, "Play services $playServices")

        userEpisodeManager.monitorUploads(applicationContext)
        downloadManager.beginMonitoringWorkManager(applicationContext)
        userManager.beginMonitoringAccountManager(playbackManager)
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(Settings.LOG_TAG_AUTO, "Terminate")
    }

    private fun setupAutomotiveDefaults() {
        // We don't want these to default to true in the main app so we set them up here.

        if (!settings.contains(Settings.PREFERENCE_AUTO_SUBSCRIBE_ON_PLAY)) {
            settings.setBooleanForKey(Settings.PREFERENCE_AUTO_SUBSCRIBE_ON_PLAY, true)
        }
        if (!settings.contains(Settings.PREFERENCE_AUTO_PLAY_ON_EMPTY)) {
            settings.setBooleanForKey(Settings.PREFERENCE_AUTO_PLAY_ON_EMPTY, true)
        }
    }

    private fun setupSentry() {
        SentryAndroid.init(this) { options ->
            options.dsn = settings.getSentryDsn()
        }
    }

    private fun setupLogging() {
        // TODO uncomment this after we have playback issues resolved
        // if (BuildConfig.DEBUG) {
        Timber.plant(TimberDebugTree())
        // }
    }

    private fun setupAnalytics() {
        AnalyticsTracker.init(settings)
    }
}
