package au.com.shiftyjelly.pocketcasts.shared

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import au.com.shiftyjelly.pocketcasts.analytics.AppLifecycleAnalytics
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationDelayCalculator
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationWorker
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.DefaultReleaseFeatureProvider
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.FirebaseRemoteFeatureProvider
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.PreferencesFeatureProvider
import au.com.shiftyjelly.pocketcasts.utils.getVersionCode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

class AppLifecycleObserver constructor(
    @ApplicationContext private val appContext: Context,
    private val appLifecycleAnalytics: AppLifecycleAnalytics,
    private val appLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
    private val applicationScope: CoroutineScope,
    private val defaultReleaseFeatureProvider: DefaultReleaseFeatureProvider,
    private val firebaseRemoteFeatureProvider: FirebaseRemoteFeatureProvider,
    private val networkConnectionWatcher: NetworkConnectionWatcherImpl,
    private val versionCode: Int,
    private val preferencesFeatureProvider: PreferencesFeatureProvider,
    private val settings: Settings,
) : DefaultLifecycleObserver {

    @Inject
    constructor(
        @ApplicationContext appContext: Context,
        @ApplicationScope applicationScope: CoroutineScope,
        appLifecycleAnalytics: AppLifecycleAnalytics,
        defaultReleaseFeatureProvider: DefaultReleaseFeatureProvider,
        networkConnectionWatcher: NetworkConnectionWatcherImpl,
        firebaseRemoteFeatureProvider: FirebaseRemoteFeatureProvider,
        preferencesFeatureProvider: PreferencesFeatureProvider,
        settings: Settings,
    ) : this(
        appContext = appContext,
        applicationScope = applicationScope,
        appLifecycleAnalytics = appLifecycleAnalytics,
        appLifecycleOwner = ProcessLifecycleOwner.get(),
        defaultReleaseFeatureProvider = defaultReleaseFeatureProvider,
        firebaseRemoteFeatureProvider = firebaseRemoteFeatureProvider,
        networkConnectionWatcher = networkConnectionWatcher,
        versionCode = appContext.getVersionCode(),
        preferencesFeatureProvider = preferencesFeatureProvider,
        settings = settings,
    )

    fun setup() {
        appLifecycleOwner.lifecycle.addObserver(this)
        handleNewInstallOrUpgrade()
        setupFeatureFlags()
        networkConnectionWatcher.startWatching()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        appLifecycleAnalytics.onApplicationEnterForeground()
        FeatureFlag.refresh()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        appLifecycleAnalytics.onApplicationEnterBackground()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        applicationScope.cancel("Application onTerminate")
        networkConnectionWatcher.stopWatching()
        super.onDestroy(owner)
    }

    private fun setupFeatureFlags() {
        val providers = if (BuildConfig.DEBUG) {
            listOf(preferencesFeatureProvider)
        } else {
            listOf(
                firebaseRemoteFeatureProvider,
                defaultReleaseFeatureProvider,
            )
        }
        FeatureFlag.initialize(providers)
    }

    private fun handleNewInstallOrUpgrade() {
        // Track app upgrade and install
        val previousVersionCode = settings.getMigratedVersionCode()

        val isNewInstall = previousVersionCode == 0
        if (isNewInstall) {
            appLifecycleAnalytics.onNewApplicationInstall()

            // new installs default to not forcing up next to use the dark theme
            settings.useDarkUpNextTheme.set(false, updateModifiedAt = false)

            // new installations default to not displaying the tooltip
            settings.showPodcastHeaderChangesTooltip.set(false, updateModifiedAt = false)
            settings.showPodcastsRecentlyPlayedSortOrderTooltip.set(false, updateModifiedAt = false)

            when (getAppPlatform()) {
                // do nothing because this already defaults to true for all users on automotive
                AppPlatform.Automotive -> {}

                // do nothing because feature has not been enabled on Wear OS yet
                AppPlatform.WearOs -> {}

                AppPlatform.Phone -> {
                    // For new users we want to auto play when the queue is empty by default
                    settings.autoPlayNextEpisodeOnEmpty.set(true, updateModifiedAt = false)

                    // For new users we want to auto download on follow podcast by default
                    settings.autoDownloadOnFollowPodcast.set(true, updateModifiedAt = false)

                    // For new users we want to enable the daily reminders notification by default
                    settings.dailyRemindersNotification.set(true, updateModifiedAt = false)

                    scheduleOnboardingNotifications(appContext)
                }
            }
        } else if (previousVersionCode < versionCode) {
            appLifecycleAnalytics.onApplicationUpgrade(previousVersionCode)
        }
    }

    @VisibleForTesting
    fun getAppPlatform() = Util.getAppPlatform(appContext)

    private fun scheduleOnboardingNotifications(context: Context) {
        val delayCalculator = NotificationDelayCalculator()

        listOf(
            OnboardingNotificationType.Sync,
            OnboardingNotificationType.Import,
            OnboardingNotificationType.UpNext,
            OnboardingNotificationType.Filters,
            OnboardingNotificationType.Themes,
            OnboardingNotificationType.StaffPicks,
            OnboardingNotificationType.PlusUpsell,
        ).forEach { type ->
            val delay = delayCalculator.calculateDelayForOnboardingNotification(type)

            val workData = workDataOf(
                "subcategory" to type.subcategory,
            )

            val notificationWork = OneTimeWorkRequest.Builder(OnboardingNotificationWorker::class.java)
                .setInputData(workData)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("onboarding_notification_${type.subcategory}")
                .build()

            WorkManager.getInstance(context).enqueue(notificationWork)
        }
    }
}
