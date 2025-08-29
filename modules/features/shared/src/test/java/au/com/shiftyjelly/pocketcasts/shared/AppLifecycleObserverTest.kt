package au.com.shiftyjelly.pocketcasts.shared

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import au.com.shiftyjelly.pocketcasts.analytics.AppLifecycleAnalytics
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.ads.BlazeAdsManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationScheduler
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.DefaultReleaseFeatureProvider
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.FirebaseRemoteFeatureProvider
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.PreferencesFeatureProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private const val VERSION_CODE_DEFAULT = 0
private const val VERSION_CODE_AFTER_FIRST_INSTALL = 1
private const val VERSION_CODE_AFTER_SECOND_INSTALL = 2

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class AppLifecycleObserverTest {

    @Mock @ApplicationContext
    private lateinit var context: Context

    @Mock private lateinit var settings: Settings

    @Mock private lateinit var autoPlayNextEpisodeSetting: UserSetting<Boolean>

    @Mock private lateinit var useUpNextDarkThemeSetting: UserSetting<Boolean>

    @Mock private lateinit var showPodcastsRecentlyPlayedSortOrderSetting: UserSetting<Boolean>

    @Mock private lateinit var showAccountEncouragementSetting: UserSetting<Boolean>

    @Mock private lateinit var autoDownloadOnFollowPodcastSetting: UserSetting<Boolean>

    @Mock private lateinit var dailyRemindersNotificationSetting: UserSetting<Boolean>

    @Mock private lateinit var notifyNewEpisodesSetting: UserSetting<Boolean>

    @Mock private lateinit var recommendationsNotificationSetting: UserSetting<Boolean>

    @Mock private lateinit var newFeaturesNotificationSetting: UserSetting<Boolean>

    @Mock private lateinit var offerNotificationSetting: UserSetting<Boolean>

    @Mock private lateinit var appLifecycleAnalytics: AppLifecycleAnalytics

    @Mock private lateinit var preferencesFeatureProvider: PreferencesFeatureProvider

    @Mock private lateinit var defaultReleaseFeatureProvider: DefaultReleaseFeatureProvider

    @Mock private lateinit var firebaseRemoteFeatureProvider: FirebaseRemoteFeatureProvider

    @Mock private lateinit var appLifecycleOwner: LifecycleOwner

    @Mock private lateinit var appLifecycle: Lifecycle

    @Mock private lateinit var networkConnectionWatcher: NetworkConnectionWatcherImpl

    @Mock private lateinit var notificationScheduler: NotificationScheduler

    @Mock private lateinit var blazeAdsManager: BlazeAdsManager

    private var coroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    lateinit var appLifecycleObserver: AppLifecycleObserver

    @Before
    fun setUp() {
        whenever(settings.autoPlayNextEpisodeOnEmpty).thenReturn(autoPlayNextEpisodeSetting)
        whenever(settings.autoDownloadOnFollowPodcast).thenReturn(autoDownloadOnFollowPodcastSetting)
        whenever(settings.dailyRemindersNotification).thenReturn(dailyRemindersNotificationSetting)
        whenever(settings.notifyRefreshPodcast).thenReturn(notifyNewEpisodesSetting)
        whenever(settings.recommendationsNotification).thenReturn(recommendationsNotificationSetting)
        whenever(settings.newFeaturesNotification).thenReturn(newFeaturesNotificationSetting)
        whenever(settings.offersNotification).thenReturn(offerNotificationSetting)
        whenever(settings.useDarkUpNextTheme).thenReturn(useUpNextDarkThemeSetting)
        whenever(settings.showPodcastsRecentlyPlayedSortOrderTooltip).thenReturn(showPodcastsRecentlyPlayedSortOrderSetting)
        whenever(settings.showFreeAccountEncouragement).thenReturn(showAccountEncouragementSetting)

        whenever(appLifecycleOwner.lifecycle).thenReturn(appLifecycle)

        appLifecycleObserver = AppLifecycleObserver(
            appContext = context,
            appLifecycleAnalytics = appLifecycleAnalytics,
            appLifecycleOwner = appLifecycleOwner,
            blazeAdsManager = blazeAdsManager,
            preferencesFeatureProvider = preferencesFeatureProvider,
            defaultReleaseFeatureProvider = defaultReleaseFeatureProvider,
            firebaseRemoteFeatureProvider = firebaseRemoteFeatureProvider,
            versionCode = VERSION_CODE_AFTER_SECOND_INSTALL,
            settings = settings,
            networkConnectionWatcher = networkConnectionWatcher,
            applicationScope = coroutineScope,
            notificationScheduler = notificationScheduler,
        )
    }

    @After
    fun tearDown() {
        coroutineScope.cancel()
    }

    /* NEW INSTALL */

    @Test
    fun handlesNewInstallPhone() = runTest {
        whenever(settings.getMigratedVersionCode()).thenReturn(VERSION_CODE_DEFAULT)

        appLifecycleObserver = spy(appLifecycleObserver)
        doReturn(AppPlatform.Phone).whenever(appLifecycleObserver).getAppPlatform()

        appLifecycleObserver.setup()

        verify(appLifecycleAnalytics).onNewApplicationInstall()
        verify(autoPlayNextEpisodeSetting).set(true, updateModifiedAt = false)
        verify(autoDownloadOnFollowPodcastSetting).set(true, updateModifiedAt = false)
        verify(dailyRemindersNotificationSetting).set(true, updateModifiedAt = false)
        verify(notifyNewEpisodesSetting).set(true, updateModifiedAt = false)
        verify(recommendationsNotificationSetting).set(true, updateModifiedAt = false)
        verify(newFeaturesNotificationSetting).set(true, updateModifiedAt = false)
        verify(offerNotificationSetting).set(true, updateModifiedAt = false)
        verify(useUpNextDarkThemeSetting).set(false, updateModifiedAt = false)

        verify(appLifecycleAnalytics, never()).onApplicationUpgrade(any())
        verify(notificationScheduler, times(1)).setupOnboardingNotifications()
        verify(notificationScheduler, times(1)).setupReEngagementNotification()
        verify(notificationScheduler, times(1)).setupTrendingAndRecommendationsNotifications()
    }

    @Test
    fun handlesNewInstallWear() = runTest {
        whenever(settings.getMigratedVersionCode()).thenReturn(VERSION_CODE_DEFAULT)

        appLifecycleObserver = spy(appLifecycleObserver)
        doReturn(AppPlatform.WearOs).whenever(appLifecycleObserver).getAppPlatform()

        appLifecycleObserver.setup()

        verify(appLifecycleAnalytics).onNewApplicationInstall()

        verify(autoPlayNextEpisodeSetting, never()).set(any(), any(), any(), any())
        verify(autoDownloadOnFollowPodcastSetting, never()).set(any(), any(), any(), any())
        verify(dailyRemindersNotificationSetting, never()).set(any(), any(), any(), any())
        verify(useUpNextDarkThemeSetting).set(false, updateModifiedAt = false)
        verify(appLifecycleAnalytics, never()).onApplicationUpgrade(any())
        verify(notificationScheduler, never()).setupOnboardingNotifications()
        verify(notificationScheduler, times(1)).setupReEngagementNotification()
        verify(notificationScheduler, times(1)).setupTrendingAndRecommendationsNotifications()
    }

    @Test
    fun handlesNewInstallAutomotive() = runTest {
        whenever(settings.getMigratedVersionCode()).thenReturn(VERSION_CODE_DEFAULT)

        appLifecycleObserver = spy(appLifecycleObserver)
        doReturn(AppPlatform.Automotive).whenever(appLifecycleObserver).getAppPlatform()

        appLifecycleObserver.setup()

        verify(appLifecycleAnalytics).onNewApplicationInstall()

        verify(autoPlayNextEpisodeSetting, never()).set(any(), any(), any(), any())
        verify(autoDownloadOnFollowPodcastSetting, never()).set(any(), any(), any(), any())
        verify(dailyRemindersNotificationSetting, never()).set(any(), any(), any(), any())
        verify(useUpNextDarkThemeSetting).set(false, updateModifiedAt = false)
        verify(appLifecycleAnalytics, never()).onApplicationUpgrade(any())
        verify(notificationScheduler, never()).setupOnboardingNotifications()
        verify(notificationScheduler, times(1)).setupReEngagementNotification()
    }

    /* UPGRADE */

    @Test
    fun handlesUpgrade() = runTest {
        whenever(settings.getMigratedVersionCode()).thenReturn(VERSION_CODE_AFTER_FIRST_INSTALL)

        appLifecycleObserver.setup()

        verify(appLifecycleAnalytics).onApplicationUpgrade(VERSION_CODE_AFTER_FIRST_INSTALL)

        verify(appLifecycleAnalytics, never()).onNewApplicationInstall()
        verify(autoPlayNextEpisodeSetting, never()).set(any(), any(), any(), any())
        verify(autoDownloadOnFollowPodcastSetting, never()).set(any(), any(), any(), any())
        verify(dailyRemindersNotificationSetting, never()).set(any(), any(), any(), any())
        verify(useUpNextDarkThemeSetting, never()).set(any(), any(), any(), any())
        verify(notificationScheduler, never()).setupOnboardingNotifications()
        verify(notificationScheduler, times(1)).setupReEngagementNotification()
        verify(notificationScheduler, times(1)).setupTrendingAndRecommendationsNotifications()
    }
}
