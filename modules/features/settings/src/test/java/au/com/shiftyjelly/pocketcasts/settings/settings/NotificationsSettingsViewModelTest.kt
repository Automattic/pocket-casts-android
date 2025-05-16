package au.com.shiftyjelly.pocketcasts.settings.settings

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.notifications.NotificationsSettingsViewModel
import au.com.shiftyjelly.pocketcasts.settings.notifications.data.NotificationsPreferenceRepository
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceCategory
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferences
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
internal class NotificationsSettingsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var repository: NotificationsPreferenceRepository

    @Mock(lenient = true)
    private lateinit var analytics: AnalyticsTracker

    @Mock
    private lateinit var podcastManager: PodcastManager

    @Before
    fun setup() {
        repository.stub {
            onBlocking { getPreferenceCategories() }.doReturn(categories)
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

        verify(repository, times(2)).getPreferenceCategories()
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
        val changedPreference = switchPreference.copy(value = !switchPreference.value)
        viewModel.onPreferenceChanged(changedPreference)

        verify(repository).setPreference(changedPreference)
        verify(analytics).track(any(), any())
        verify(repository, times(2)).getPreferenceCategories()
    }

    private fun createViewModel() = NotificationsSettingsViewModel(
        preferenceRepository = repository,
        analyticsTracker = analytics,
        podcastManager = podcastManager,
    )

    private companion object {
        val categories = listOf(
            NotificationPreferenceCategory(
                title = "category1",
                preferences = listOf(
                    NotificationPreference.SwitchPreference(
                        title = "switch",
                        value = false,
                        preference = NotificationPreferences.SETTINGS_HIDE_NOTIFICATION_ON_PAUSE,
                    ),
                ),
            ),
        )

        val switchPreference = NotificationPreference.SwitchPreference(
            title = "switch",
            value = false,
            preference = NotificationPreferences.SETTINGS_HIDE_NOTIFICATION_ON_PAUSE,
        )

        val otherCategories = listOf(
            NotificationPreferenceCategory(
                title = "category1",
                preferences = listOf(
                    switchPreference,
                ),
            ),
            NotificationPreferenceCategory(
                title = "category2",
                preferences = listOf(
                    NotificationPreference.TextPreference(
                        title = "text",
                        value = "text value",
                        preference = NotificationPreferences.SETTINGS_PLAY_OVER,
                    ),
                    NotificationPreference.RadioGroupPreference(
                        title = "radio",
                        value = 1,
                        preference = NotificationPreferences.NEW_EPISODES_ACTIONS,
                        options = (1..3).toList(),
                        displayText = "one",
                    ),
                ),
            ),
        )
    }
}
