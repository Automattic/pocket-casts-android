package au.com.shiftyjelly.pocketcasts.settings.settings

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationScheduler
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.notifications.NotificationsSettingsViewModel
import au.com.shiftyjelly.pocketcasts.settings.notifications.data.NotificationsPreferenceRepository
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceCategory
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceType
import au.com.shiftyjelly.pocketcasts.settings.util.TextResource
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
internal class NotificationsSettingsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var repository: NotificationsPreferenceRepository

    @Mock(lenient = true)
    private lateinit var analytics: AnalyticsTracker

    @Mock
    private lateinit var podcastManager: PodcastManager

    @Mock
    private lateinit var notificationScheduler: NotificationScheduler

    @Mock
    private lateinit var notificationHelper: NotificationHelper

    @Before
    fun setup() {
        repository.stub {
            onBlocking { getPreferenceCategories() }.doReturn(categories)
        }
        notificationHelper.stub {
            onBlocking { hasNotificationsPermission() }.doReturn(false)
        }
    }

    @Test
    fun `GIVEN repository holds valid data WHEN vm is created THEN state updates`() = runTest {
        val viewModel = createViewModel()

        assertEquals(viewModel.state.value.categories, categories)
        verify(repository).getPreferenceCategories()
    }

    @Test
    fun `GIVEN repository has new data WHEN querying preferences THEN state shows latest data`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            skipItems(1)
            whenever(repository.getPreferenceCategories()).doReturn(otherCategories)

            viewModel.loadPreferences()
            val state = awaitItem()

            assertEquals(state.categories, otherCategories)
        }
    }

    @Test
    fun `GIVEN vm created WHEN onShow called THEN tracker invoked`() = runTest {
        val viewModel = createViewModel()

        viewModel.onShown()

        verify(analytics).track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_SHOWN)
    }

    @Test
    fun `GIVEN preference WHEN preference changes THEN repository called to persist change`() = runTest {
        val viewModel = createViewModel()
        val changedPreference = notifyMe.copy(isEnabled = !notifyMe.isEnabled)
        viewModel.onPreferenceChanged(changedPreference)

        verify(repository).setPreference(changedPreference)
    }

    @Test
    fun `GIVEN disabled recommendations WHEN recommendations are enabled THEN scheduling notifications`() = runTest {
        val viewModel = createViewModel()
        val preference = NotificationPreferenceType.EnableRecommendations(title = TextResource.fromText(""), isEnabled = true)

        viewModel.onPreferenceChanged(preference)

        verify(notificationScheduler).setupTrendingAndRecommendationsNotifications()
    }

    @Test
    fun `GIVEN enabled recommendations WHEN recommendations are enabled THEN cancelling scheduled notifications`() = runTest {
        val viewModel = createViewModel()
        val preference = NotificationPreferenceType.EnableRecommendations(title = TextResource.fromText(""), isEnabled = false)

        viewModel.onPreferenceChanged(preference)

        verify(notificationScheduler).cancelScheduledTrendingAndRecommendationsNotifications()
    }

    @Test
    fun `GIVEN notifications disabled WHEN initializing viewmodel THEN state reflects it`() = runTest {
        val viewModel = createViewModel()

        assertEquals(viewModel.state.value.areSystemNotificationsEnabled, false)
    }

    @Test
    fun `GIVEN notifications enabled WHEN initializing viewmodel THEN state reflects it`() = runTest {
        notificationHelper.stub {
            onBlocking { hasNotificationsPermission() }.doReturn(true)
        }

        val viewModel = createViewModel()

        assertEquals(viewModel.state.value.areSystemNotificationsEnabled, true)
    }

    @Test
    fun `GIVEN notification setting changes WHEN viewmodel resumes THEN state updates`() = runTest {
        val viewModel = createViewModel()
        assertEquals(viewModel.state.value.areSystemNotificationsEnabled, false)

        notificationHelper.stub {
            onBlocking { hasNotificationsPermission() }.doReturn(true)
        }
        viewModel.checkNotificationPermission()

        assertEquals(viewModel.state.value.areSystemNotificationsEnabled, true)
    }

    private fun createViewModel() = NotificationsSettingsViewModel(
        preferenceRepository = repository,
        analyticsTracker = analytics,
        podcastManager = podcastManager,
        notificationScheduler = notificationScheduler,
        notificationHelper = notificationHelper,
    )

    private companion object {
        val notifyMe = NotificationPreferenceType.NotifyMeOnNewEpisodes(
            title = TextResource.fromText("switch"),
            isEnabled = false,
        )
        val categories = listOf(
            NotificationPreferenceCategory(
                title = TextResource.fromText("category1"),
                preferences = listOf(
                    notifyMe,
                ),
            ),
        )
        val otherCategories = listOf(
            NotificationPreferenceCategory(
                title = TextResource.fromText("category1"),
                preferences = listOf(
                    notifyMe,
                ),
            ),
            NotificationPreferenceCategory(
                title = TextResource.fromText("category2"),
                preferences = listOf(
                    NotificationPreferenceType.HidePlaybackNotificationOnPause(
                        title = TextResource.fromText("text"),
                        isEnabled = true,
                    ),
                    NotificationPreferenceType.PlayOverNotifications(
                        title = TextResource.fromText("item 2"),
                        value = PlayOverNotificationSetting.DUCK,
                        displayValue = TextResource.fromText("duck"),
                        options = emptyList(),
                    ),
                ),
            ),
        )
    }
}
