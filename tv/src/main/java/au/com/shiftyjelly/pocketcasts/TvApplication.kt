package au.com.shiftyjelly.pocketcasts

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsController
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackServiceToggle
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.TimberDebugTree
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.DefaultReleaseFeatureProvider
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.FirebaseRemoteFeatureProvider
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.PreferencesFeatureProvider
import au.com.shiftyjelly.pocketcasts.utils.log.RxJavaUncaughtExceptionHandling
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber

@HiltAndroidApp
class TvApplication :
    Application(),
    Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var notificationHelper: NotificationHelper

    @Inject lateinit var defaultReleaseFeatureProvider: DefaultReleaseFeatureProvider

    @Inject lateinit var firebaseRemoteFeatureProvider: FirebaseRemoteFeatureProvider

    @Inject lateinit var preferencesFeatureProvider: PreferencesFeatureProvider

    @Inject lateinit var analyticsController: AnalyticsController

    @Inject lateinit var userManager: UserManager

    @Inject lateinit var appLifecycleObserver: TvAppLifecycleObserver

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(TimberDebugTree())
        }
        RxJavaUncaughtExceptionHandling.setUp()
        setupFeatureFlags()
        setupAnalytics()
        notificationHelper.setupNotificationChannels()
        appLifecycleObserver.setup()
        PlaybackServiceToggle.ensureCorrectServiceEnabled(this)
        // setup() subscribes the Up Next queue's sync pipeline itself, so there must be no
        // separate UpNextQueue.setupBlocking() call on TV.
        applicationScope.launch {
            playbackManager.setup()
        }
    }

    private fun setupAnalytics() {
        analyticsController.clearAllData()
        analyticsController.refreshMetadata()
        applicationScope.launch {
            userManager.getSignInState().toObservable().asFlow()
                .distinctUntilChanged()
                .collect { analyticsController.refreshMetadata() }
        }
    }

    private fun setupFeatureFlags() {
        val providers = if (BuildConfig.DEBUG || BuildConfig.IS_PROTOTYPE) {
            listOf(preferencesFeatureProvider)
        } else {
            listOf(
                firebaseRemoteFeatureProvider,
                defaultReleaseFeatureProvider,
            )
        }
        FeatureFlag.initialize(providers)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
