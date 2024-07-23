package au.com.shiftyjelly.pocketcasts.sharing

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_INTENT
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.EXTRA_TITLE
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.Tracker
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import java.io.File
import java.util.Date
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ShareActionsTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val shareStarter = TestShareStarter()

    private val tracker = TestTracker()

    private val actions = ShareActions(
        context = context,
        tracker = AnalyticsTracker.test(tracker, isEnabled = true),
        source = SourceView.BOTTOM_SHELF,
        displayPodcastCover = false,
        hostUrl = "https://pca.st",
        shareStarter = shareStarter,
    )

    @Test
    fun sharePodcast() = runTest {
        actions.sharePodcast(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
        )

        val intent = shareStarter.shareIntent

        assertEquals(ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("https://pca.st/podcast/podcast-uuid", intent.getStringExtra(EXTRA_TEXT))
        assertEquals("Podcast Title", intent.getStringExtra(EXTRA_TITLE))
        assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
    }

    @Test
    fun shareEpisode() = runTest {
        actions.shareEpisode(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
        )

        val intent = shareStarter.shareIntent

        assertEquals(ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("https://pca.st/episode/episode-uuid", intent.getStringExtra(EXTRA_TEXT))
        assertEquals("Episode Title", intent.getStringExtra(EXTRA_TITLE))
        assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
    }

    @Test
    fun shareEpisodePosition() = runTest {
        actions.shareEpisodePosition(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            start = 25.seconds,
        )

        val intent = shareStarter.shareIntent

        assertEquals(ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("https://pca.st/episode/episode-uuid?t=25", intent.getStringExtra(EXTRA_TEXT))
        assertEquals("Episode Title", intent.getStringExtra(EXTRA_TITLE))
        assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
    }

    @Test
    fun shareBookmark() = runTest {
        actions.shareBookmark(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            start = 25.seconds,
        )

        val intent = shareStarter.shareIntent

        assertEquals(ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("https://pca.st/episode/episode-uuid?t=25", intent.getStringExtra(EXTRA_TEXT))
        assertEquals("Episode Title", intent.getStringExtra(EXTRA_TITLE))
        assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
    }

    @Test
    fun shareClipLink() = runTest {
        actions.shareClipLink(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            start = 25.seconds,
            end = 85.seconds,
        )

        val intent = shareStarter.shareIntent

        assertEquals(ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("https://pca.st/episode/episode-uuid?t=25,85", intent.getStringExtra(EXTRA_TEXT))
        assertEquals("Episode Title", intent.getStringExtra(EXTRA_TITLE))
        assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
    }

    @Test
    fun shareEpisodeFile() = runTest {
        val file = File(context.cacheDir, "file.mp3")

        actions.shareEpisodeFile(
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", downloadedFilePath = file.path, fileType = "audio/mp3", publishedDate = Date()),
        )

        val intent = shareStarter.shareIntent

        assertEquals(ACTION_SEND, intent.action)
        assertEquals("audio/mp3", intent.type)
        assertEquals(FileUtil.getUriForFile(context, file), IntentCompat.getParcelableExtra(intent, EXTRA_STREAM, Uri::class.java))
    }

    @Test
    fun sharePodcastAnalytics() = runTest {
        actions.sharePodcast(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
        )

        val (event, properties) = tracker.events.single()

        assertEquals(AnalyticsEvent.PODCAST_SHARED, event)
        assertEquals(
            mapOf(
                "source" to SourceView.BOTTOM_SHELF.analyticsValue,
                "type" to "podcast",
            ),
            properties,
        )
    }

    @Test
    fun shareEpisodeAnalytics() = runTest {
        actions.shareEpisode(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
        )

        val (event, properties) = tracker.events.single()

        assertEquals(AnalyticsEvent.PODCAST_SHARED, event)
        assertEquals(
            mapOf(
                "source" to SourceView.BOTTOM_SHELF.analyticsValue,
                "type" to "episode",
            ),
            properties,
        )
    }

    @Test
    fun shareEpisodePositionAnalytics() = runTest {
        actions.shareEpisodePosition(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            start = 25.seconds,
        )

        val (event, properties) = tracker.events.single()

        assertEquals(AnalyticsEvent.PODCAST_SHARED, event)
        assertEquals(
            mapOf(
                "source" to SourceView.BOTTOM_SHELF.analyticsValue,
                "type" to "current_time",
            ),
            properties,
        )
    }

    @Test
    fun shareBookmarkAnalytics() = runTest {
        actions.shareBookmark(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            start = 25.seconds,
        )

        val (event, properties) = tracker.events.single()

        assertEquals(AnalyticsEvent.PODCAST_SHARED, event)
        assertEquals(
            mapOf(
                "source" to SourceView.BOTTOM_SHELF.analyticsValue,
                "type" to "bookmark_time",
            ),
            properties,
        )
    }

    @Test
    fun shareClipLinkAnalytics() = runTest {
        actions.shareClipLink(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            start = 25.seconds,
            end = 85.seconds,
        )

        val (event, properties) = tracker.events.single()

        assertEquals(AnalyticsEvent.PODCAST_SHARED, event)
        assertEquals(
            mapOf(
                "source" to SourceView.BOTTOM_SHELF.analyticsValue,
                "type" to "clip_link",
            ),
            properties,
        )
    }

    @Test
    fun shareEpisodeFileAnalytics() = runTest {
        val file = File(context.cacheDir, "file.mp3")

        actions.shareEpisodeFile(
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", downloadedFilePath = file.path, fileType = "audio/mp3", publishedDate = Date()),
        )

        val (event, properties) = tracker.events.single()

        assertEquals(AnalyticsEvent.PODCAST_SHARED, event)
        assertEquals(
            mapOf(
                "source" to SourceView.BOTTOM_SHELF.analyticsValue,
                "type" to "episode_file",
            ),
            properties,
        )
    }

    @Test
    fun trackSharingWithPodcastScreenSource() = runTest {
        val actions = ShareActions(
            context = context,
            tracker = AnalyticsTracker.test(tracker, isEnabled = true),
            source = SourceView.PODCAST_SCREEN,
            displayPodcastCover = true,
            hostUrl = "https://pca.st",
            shareStarter = shareStarter,
        )

        actions.sharePodcast(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
        )

        val (event1, properties1) = tracker.events[0]

        assertEquals(AnalyticsEvent.PODCAST_SHARED, event1)
        assertEquals(
            mapOf(
                "source" to SourceView.PODCAST_SCREEN.analyticsValue,
                "type" to "podcast",
            ),
            properties1,
        )

        val (event2, properties2) = tracker.events[1]

        assertEquals(AnalyticsEvent.PODCAST_SCREEN_SHARE_TAPPED, event2)
        assertEquals(emptyMap<String, Any>(), properties2)
    }

    private class TestShareStarter : ShareStarter {
        private var _intent: Intent? = null
        val shareIntent get() = requireNotNull(IntentCompat.getParcelableExtra(requireNotNull(_intent), EXTRA_INTENT, Intent::class.java))

        override fun start(context: Context, intent: Intent) {
            _intent = intent
        }
    }

    private class TestTracker : Tracker {
        private val _trackedEvents = mutableListOf<Pair<AnalyticsEvent, Map<String, Any>>>()
        val events get() = _trackedEvents.toList()

        override fun track(event: AnalyticsEvent, properties: Map<String, Any>) {
            _trackedEvents.add(event to properties)
        }

        override fun refreshMetadata() = Unit

        override fun flush() = Unit

        override fun clearAllData() = Unit
    }
}
