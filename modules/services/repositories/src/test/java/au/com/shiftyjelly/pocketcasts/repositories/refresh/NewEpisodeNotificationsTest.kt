package au.com.shiftyjelly.pocketcasts.repositories.refresh

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.NotificationSound
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationAction
import au.com.shiftyjelly.pocketcasts.preferences.model.NotificationVibrateSetting
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.NotificationBroadcastReceiver
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import java.util.Date
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import timber.log.Timber

@OptIn(DelicateCoilApi::class)
@Config(manifest = Config.NONE, sdk = [34])
@RunWith(RobolectricTestRunner::class)
class NewEpisodeNotificationsTest {
    private val application: Application = RuntimeEnvironment.getApplication()
    private val appResources = spy(application.resources) {
        doReturn(0).whenever(it).getColor(any(), anyOrNull())
        doReturn("Archive").whenever(it).getString(any())
    }
    private val context: Context = object : ContextWrapper(application) {
        override fun getResources(): Resources = appResources
    }
    private val prefs = context.getSharedPreferences("new_episode_notifications_test", Context.MODE_PRIVATE)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    private val notificationActions = UserSetting.Mock(listOf(NewEpisodeNotificationAction.Archive), prefs)
    private val settings = mock<Settings> {
        on { newEpisodeNotificationActions } doReturn notificationActions
        on { notificationSound } doReturn UserSetting.Mock(NotificationSound("", context), prefs)
        on { notificationVibrate } doReturn UserSetting.Mock(NotificationVibrateSetting.Never, prefs)
        on { artworkConfiguration } doReturn UserSetting.Mock(ArtworkConfiguration(useEpisodeArtwork = false), prefs)
    }
    private val notificationHelper = mock<NotificationHelper> {
        on { episodeNotificationChannelBuilder() } doAnswer { NotificationCompat.Builder(context, "episode") }
    }

    private val podcast1 = Podcast(uuid = "podcast-1", title = "Podcast 1", isSubscribed = true, isShowNotifications = true)
    private val podcast2 = Podcast(uuid = "podcast-2", title = "Podcast 2", isSubscribed = true, isShowNotifications = true)

    private val loggedErrors = mutableListOf<Throwable>()
    private val errorTree = object : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            t?.let(loggedErrors::add)
        }
    }

    @Before
    fun setUp() {
        Timber.plant(errorTree)
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(application)
                .components { add(Interceptor { chain -> ErrorResult(image = null, request = chain.request, throwable = IllegalStateException()) }) }
                .build(),
        )
        val launcher = ComponentName(context.packageName, "Launcher")
        shadowOf(context.packageManager).addActivityIfNotPresent(launcher)
        shadowOf(context.packageManager).addIntentFilterForActivity(
            launcher,
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
        )
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun tearDown() {
        Timber.uproot(errorTree)
        SingletonImageLoader.reset()
        assertEquals(emptyList<Throwable>(), loggedErrors)
    }

    @Test
    fun `single episodes from separate refreshes are both shown`() {
        refresh(episode("episode-1", podcast1))
        refresh(episode("episode-2", podcast2))

        assertEquals(setOf(tag("episode-1"), tag("episode-2")), activeTags())
    }

    @Test
    fun `single episode survives a later grouped refresh`() {
        refresh(episode("episode-1", podcast1))
        refresh(episode("episode-2", podcast2), episode("episode-3", podcast2))

        assertTrue(tag("episode-1") in activeTags())
        assertTrue(activeNotifications().any { it.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 })
    }

    @Test
    fun `later single episode does not replace a group summary`() {
        refresh(episode("episode-1", podcast1), episode("episode-2", podcast1))
        refresh(episode("episode-3", podcast2))

        assertEquals(
            setOf(NotificationBroadcastReceiver.NOTIFICATION_TAG_NEW_EPISODES_PRIMARY, tag("episode-1"), tag("episode-2"), tag("episode-3")),
            activeTags(),
        )
    }

    @Test
    fun `episodes in the same refresh get their own pending intents`() {
        refresh(episode("episode-1", podcast1), episode("episode-2", podcast2))

        val episodeNotifications = activeNotifications().filter { it.tag != NotificationBroadcastReceiver.NOTIFICATION_TAG_NEW_EPISODES_PRIMARY }
        val contentRequestCodes = episodeNotifications.map { shadowOf(it.notification.contentIntent).requestCode }
        val actionRequestCodes = episodeNotifications.map { shadowOf(it.notification.actions.single().actionIntent).requestCode }

        assertEquals(2, contentRequestCodes.toSet().size)
        assertEquals(2, actionRequestCodes.toSet().size)
    }

    @Test
    fun `action buttons cancel the notification they belong to`() {
        refresh(episode("episode-1", podcast1))
        refresh(episode("episode-2", podcast2), episode("episode-3", podcast2))

        activeNotifications()
            .filter { it.tag != NotificationBroadcastReceiver.NOTIFICATION_TAG_NEW_EPISODES_PRIMARY }
            .forEach { posted ->
                val actionIntent = shadowOf(posted.notification.actions.single().actionIntent).savedIntent
                assertEquals(posted.tag, actionIntent.getStringExtra(NotificationBroadcastReceiver.INTENT_EXTRA_NOTIFICATION_TAG))
            }
    }

    private fun refresh(vararg episodes: Pair<PodcastEpisode, Podcast>) {
        val podcastManager = mock<PodcastManager> {
            on { countNotificationsOnBlocking() } doReturn 2
        }
        episodes.forEach { (_, podcast) ->
            whenever(podcastManager.findPodcastByUuidBlocking(podcast.uuid)).doReturn(podcast)
        }
        val episodeManager = mock<EpisodeManager> {
            on { findNotificationEpisodesBlocking(any()) } doReturn episodes.map { it.first }
        }
        RefreshPodcastsThread.updateNotifications(Date(0), settings, podcastManager, episodeManager, notificationHelper, context)
    }

    private fun episode(uuid: String, podcast: Podcast) = PodcastEpisode(uuid = uuid, podcastUuid = podcast.uuid, title = uuid, publishedDate = Date()) to podcast

    private fun tag(episodeUuid: String) = NotificationBroadcastReceiver.NOTIFICATION_TAG_NEW_EPISODES_PREFIX + episodeUuid

    private fun activeNotifications(): List<StatusBarNotification> = shadowOf(notificationManager).activeNotifications.toList()

    private fun activeTags() = activeNotifications().map { it.tag }.toSet()
}
