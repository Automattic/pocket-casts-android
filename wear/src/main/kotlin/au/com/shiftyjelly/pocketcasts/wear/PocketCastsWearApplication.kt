package au.com.shiftyjelly.pocketcasts.wear

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import au.com.shiftyjelly.pocketcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.TimberDebugTree
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltAndroidApp
class PocketCastsWearApplication : Application(), Configuration.Provider {

    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var userManager: UserManager
    @Inject lateinit var settings: Settings
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        setupLogging()
        setupAnalytics()
        setupApp()
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
            withContext(Dispatchers.Default) {
                playbackManager.setup()
            }
        }
        userManager.beginMonitoringAccountManager(playbackManager)
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
