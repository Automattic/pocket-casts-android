package au.com.shiftyjelly.pocketcasts.wear

import android.app.Application
import au.com.shiftyjelly.pocketcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.TimberDebugTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class PocketCastsWearApplication : Application() {

    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var userManager: UserManager

    override fun onCreate() {
        super.onCreate()

        setupLogging()
        setupApp()
    }

    private fun setupLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(TimberDebugTree())
        }
    }

    private fun setupApp() {
        userManager.beginMonitoringAccountManager(playbackManager)
    }
}
