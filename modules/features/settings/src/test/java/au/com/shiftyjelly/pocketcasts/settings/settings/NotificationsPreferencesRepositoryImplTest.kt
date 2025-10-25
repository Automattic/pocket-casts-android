package au.com.shiftyjelly.pocketcasts.settings.settings

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.NotificationSound
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationAction
import au.com.shiftyjelly.pocketcasts.preferences.model.NotificationVibrateSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.notifications.data.NotificationFeaturesProvider
import au.com.shiftyjelly.pocketcasts.settings.notifications.data.NotificationsPreferencesRepositoryImpl
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceType
import au.com.shiftyjelly.pocketcasts.settings.util.TextResource
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class NotificationsPreferencesRepositoryImplTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var podcastManager: PodcastManager

    @Before
    fun setup() {
        whenever(context.getString(any())).doReturn("")
        whenever(podcastManager.findSubscribedFlow(anyOrNull())).thenReturn(emptyFlow())
        whenever(settings.notifyRefreshPodcast).thenReturn(UserSetting.Mock(false, mock()))
        whenever(settings.newEpisodeNotificationActions).thenReturn(UserSetting.Mock(NewEpisodeNotificationAction.DefaultValues, mock()))
        whenever(settings.playOverNotification).thenReturn(UserSetting.Mock(PlayOverNotificationSetting.DUCK, mock()))
        whenever(settings.hideNotificationOnPause).thenReturn(UserSetting.Mock(false, mock()))
        whenever(settings.dailyRemindersNotification).thenReturn(UserSetting.Mock(false, mock()))
        whenever(settings.recommendationsNotification).thenReturn(UserSetting.Mock(false, mock()))
        whenever(settings.newFeaturesNotification).thenReturn(UserSetting.Mock(false, mock()))
        whenever(settings.offersNotification).thenReturn(UserSetting.Mock(false, mock()))
    }

    @Test
    fun `GIVEN notify me disabled WHEN categories queried THEN no other episode settings are present`() = runTest {
        val repository = createRepository()

        val categories = repository.getPreferenceCategories()

        assert(categories.find { it.preferences.any { it is NotificationPreferenceType.NotifyMeOnNewEpisodes } }?.preferences?.size == 1)
    }

    @Test
    fun `GIVEN notification channels WHEN categories queried THEN advanced settings present`() = runTest {
        settings.stub {
            whenever(settings.notifyRefreshPodcast).thenReturn(UserSetting.Mock(true, mock()))
        }
        val repository = createRepository()

        val categories = repository.getPreferenceCategories()

        assert(categories.map { it.preferences }.flatten().any { it is NotificationPreferenceType.AdvancedSettings })
    }

    @Test
    fun `GIVEN no notification channels WHEN categories queried THEN vibrate and ringtone are present`() = runTest {
        settings.stub {
            whenever(settings.notifyRefreshPodcast).thenReturn(UserSetting.Mock(true, mock()))
        }
        val repository = createRepository(hasNotificationChannels = false)

        val categories = repository.getPreferenceCategories()

        assert(
            categories.map { it.preferences }.flatten()
                .filter {
                    it is NotificationPreferenceType.NotificationSoundPreference || it is NotificationPreferenceType.NotificationVibration
                }.size == 2,
        )
    }

    @Test
    fun `GIVEN new preference value WHEN setting a preference THEN setting is called`() = runTest {
        val repository = createRepository()

        repository.setPreference(
            NotificationPreferenceType.NotifyMeOnNewEpisodes(
                title = TextResource.fromText(""),
                isEnabled = true,
            ),
        )

        verify(podcastManager).updateAllShowNotifications(true)
        verify(settings).setNotificationLastSeenToNow()
    }

    @Test
    fun `GIVEN feature off WHEN categories queried THEN enable daily reminder toggle is absent`() = runTest {
        val repository = createRepository(isRevampFeatureEnabled = false)

        val categories = repository.getPreferenceCategories()

        assert(categories.map { it.preferences }.flatten().filterIsInstance<NotificationPreferenceType.EnableDailyReminders>().isEmpty())
    }

    @Test
    fun `GIVEN feature ON WHEN categories queried THEN daily reminder settings appear when expected`() = runTest {
        val repository = createRepository(isRevampFeatureEnabled = true)
        val categories = repository.getPreferenceCategories()
        assert(categories.map { it.preferences }.flatten().filterIsInstance<NotificationPreferenceType.EnableDailyReminders>().isNotEmpty())
        assert(categories.map { it.preferences }.flatten().filterIsInstance<NotificationPreferenceType.DailyReminderSettings>().isEmpty())

        whenever(settings.dailyRemindersNotification).thenReturn(UserSetting.Mock(true, mock()))
        val updatedCategories = repository.getPreferenceCategories()
        assert(updatedCategories.map { it.preferences }.flatten().filterIsInstance<NotificationPreferenceType.DailyReminderSettings>().isNotEmpty())
    }

    @Test
    fun `GIVEN feature off WHEN categories queried THEN enable trending and recommendations toggle is absent`() = runTest {
        val repository = createRepository(isRevampFeatureEnabled = false)

        val categories = repository.getPreferenceCategories()

        assert(categories.map { it.preferences }.flatten().filterIsInstance<NotificationPreferenceType.EnableRecommendations>().isEmpty())
    }

    @Test
    fun `GIVEN feature ON WHEN categories queried THEN trending and recommendations settings appear when expected`() = runTest {
        val repository = createRepository(isRevampFeatureEnabled = true)
        val categories = repository.getPreferenceCategories()
        assert(categories.map { it.preferences }.flatten().filterIsInstance<NotificationPreferenceType.EnableRecommendations>().isNotEmpty())
        assert(categories.map { it.preferences }.flatten().filterIsInstance<NotificationPreferenceType.RecommendationSettings>().isEmpty())

        whenever(settings.recommendationsNotification).thenReturn(UserSetting.Mock(true, mock()))
        val updatedCategories = repository.getPreferenceCategories()
        assert(updatedCategories.map { it.preferences }.flatten().filterIsInstance<NotificationPreferenceType.RecommendationSettings>().isNotEmpty())
    }

    private fun createRepository(
        hasNotificationChannels: Boolean = true,
        isRevampFeatureEnabled: Boolean = true,
    ): NotificationsPreferencesRepositoryImpl {
        if (!hasNotificationChannels) {
            val notificationSound = mock<NotificationSound>(lenient = true) {
                on { uri } doReturn mock()
                on { path } doReturn ""
            }
            whenever(settings.notificationSound).thenReturn(UserSetting.Mock(notificationSound, mock()))
            whenever(settings.notificationVibrate).thenReturn(UserSetting.Mock(NotificationVibrateSetting.Never, mock()))
        }

        return NotificationsPreferencesRepositoryImpl(
            context = context,
            settings = settings,
            podcastManager = podcastManager,
            notificationFeaturesProvider = NotificationFeaturesProvider(
                hasNotificationChannels = hasNotificationChannels,
                isRevampFeatureEnabled = isRevampFeatureEnabled,
            ),
        )
    }
}
