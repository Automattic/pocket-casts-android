package au.com.shiftyjelly.pocketcasts.sharing

import android.content.ClipData
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
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.clip.Clip
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import java.io.File
import java.util.Date
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SharingClientTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val shareStarter = TestShareStarter()

    private val client = createClient()

    private val regularPlatforms = SocialPlatform.entries - SocialPlatform.Instagram - SocialPlatform.PocketCasts

    @Test
    fun sharePodcastToRegularPlatforms() = runTest {
        regularPlatforms.forEach { platform ->
            val request = SharingRequest.podcast(Podcast(uuid = "podcast-uuid", title = "Podcast Title"))
                .setPlatform(platform)
                .build()

            client.share(request)
            val intent = shareStarter.requireShareIntent

            assertEquals(ACTION_SEND, intent.action)
            assertEquals("text/plain", intent.type)
            assertEquals("https://pca.st/podcast/podcast-uuid", intent.getStringExtra(EXTRA_TEXT))
            assertEquals("Podcast Title", intent.getStringExtra(EXTRA_TITLE))
            assertEquals(platform.packageId, intent.`package`)
            assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    @Test
    fun copyPodcastLink() = runTest {
        val request = SharingRequest.podcast(Podcast(uuid = "podcast-uuid", title = "Podcast Title"))
            .setPlatform(SocialPlatform.PocketCasts)
            .build()

        client.share(request)
        val clipData = shareStarter.requireShareLink

        assertEquals("https://pca.st/podcast/podcast-uuid", clipData.getItemAt(0).text)
        assertEquals(context.getString(LR.string.share_link_podcast), clipData.description.label)
        assertNull(shareStarter.shareIntent)
    }

    @Test
    fun copyPodcastLinkWithFeedback() = runTest {
        val client = createClient(showCustomCopyFeedback = true)

        val request = SharingRequest.podcast(Podcast(uuid = "podcast-uuid", title = "Podcast Title"))
            .setPlatform(SocialPlatform.PocketCasts)
            .build()

        val response = client.share(request)

        assertEquals(context.getString(LR.string.share_link_copied_feedback), response.feedbackMessage)
    }

    @Test
    fun copyPodcastLinkWithoutFeedback() = runTest {
        val client = createClient(showCustomCopyFeedback = false)

        val request = SharingRequest.podcast(Podcast(uuid = "podcast-uuid", title = "Podcast Title"))
            .setPlatform(SocialPlatform.PocketCasts)
            .build()

        val response = client.share(request)

        assertNull(response.feedbackMessage)
    }

    @Test
    fun shareEpisodeToRegularPlatforms() = runTest {
        regularPlatforms.forEach { platform ->
            val request = SharingRequest.episode(
                podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
                episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            ).setPlatform(platform)
                .build()

            client.share(request)
            val intent = shareStarter.requireShareIntent

            assertEquals(ACTION_SEND, intent.action)
            assertEquals("text/plain", intent.type)
            assertEquals("https://pca.st/episode/episode-uuid", intent.getStringExtra(EXTRA_TEXT))
            assertEquals("Episode Title", intent.getStringExtra(EXTRA_TITLE))
            assertEquals(platform.packageId, intent.`package`)
            assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    @Test
    fun copyEpisodeLink() = runTest {
        val request = SharingRequest.episode(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
        ).setPlatform(SocialPlatform.PocketCasts)
            .build()

        client.share(request)
        val clipData = shareStarter.requireShareLink

        assertEquals("https://pca.st/episode/episode-uuid", clipData.getItemAt(0).text)
        assertEquals(context.getString(LR.string.share_link_episode), clipData.description.label)
        assertNull(shareStarter.shareIntent)
    }

    @Test
    fun copyEpisodeLinkWithFeedback() = runTest {
        val client = createClient(showCustomCopyFeedback = true)

        val request = SharingRequest.episode(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
        ).setPlatform(SocialPlatform.PocketCasts)
            .build()

        val response = client.share(request)

        assertEquals(context.getString(LR.string.share_link_copied_feedback), response.feedbackMessage)
    }

    @Test
    fun copyEpisodeLinkWithoutFeedback() = runTest {
        val client = createClient(showCustomCopyFeedback = false)

        val request = SharingRequest.episode(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
        ).setPlatform(SocialPlatform.PocketCasts)
            .build()

        val response = client.share(request)

        assertNull(response.feedbackMessage)
    }

    @Test
    fun shareEpisodePositionToRegularPlatforms() = runTest {
        regularPlatforms.forEach { platform ->
            val request = SharingRequest.episodePosition(
                podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
                episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
                position = 10.seconds,
            ).setPlatform(platform)
                .build()

            client.share(request)
            val intent = shareStarter.requireShareIntent

            assertEquals(ACTION_SEND, intent.action)
            assertEquals("text/plain", intent.type)
            assertEquals("https://pca.st/episode/episode-uuid?t=10", intent.getStringExtra(EXTRA_TEXT))
            assertEquals("Episode Title", intent.getStringExtra(EXTRA_TITLE))
            assertEquals(platform.packageId, intent.`package`)
            assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    @Test
    fun copyEpisodePositionLink() = runTest {
        val request = SharingRequest.episodePosition(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            position = 10.seconds,
        ).setPlatform(SocialPlatform.PocketCasts)
            .build()

        client.share(request)
        val clipData = shareStarter.requireShareLink

        assertEquals("https://pca.st/episode/episode-uuid?t=10", clipData.getItemAt(0).text)
        assertEquals(context.getString(LR.string.share_link_episode_position), clipData.description.label)
        assertNull(shareStarter.shareIntent)
    }

    @Test
    fun copyEpisodePositionLinkWithFeedback() = runTest {
        val client = createClient(showCustomCopyFeedback = true)

        val request = SharingRequest.episodePosition(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            position = 10.seconds,
        ).setPlatform(SocialPlatform.PocketCasts)
            .build()

        val response = client.share(request)

        assertEquals(context.getString(LR.string.share_link_copied_feedback), response.feedbackMessage)
    }

    @Test
    fun copyEpisodePositionLinkWithoutFeedback() = runTest {
        val client = createClient(showCustomCopyFeedback = false)

        val request = SharingRequest.episodePosition(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            position = 10.seconds,
        ).setPlatform(SocialPlatform.PocketCasts)
            .build()

        val response = client.share(request)

        assertNull(response.feedbackMessage)
    }

    @Test
    fun shareBookmarkToRegularPlatforms() = runTest {
        regularPlatforms.forEach { platform ->
            val request = SharingRequest.bookmark(
                podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
                episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
                position = 10.seconds,
            ).setPlatform(platform)
                .build()

            client.share(request)
            val intent = shareStarter.requireShareIntent

            assertEquals(ACTION_SEND, intent.action)
            assertEquals("text/plain", intent.type)
            assertEquals("https://pca.st/episode/episode-uuid?t=10", intent.getStringExtra(EXTRA_TEXT))
            assertEquals("Episode Title", intent.getStringExtra(EXTRA_TITLE))
            assertEquals(platform.packageId, intent.`package`)
            assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    @Test
    fun copyBookmarkLink() = runTest {
        val request = SharingRequest.bookmark(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            position = 10.seconds,
        ).setPlatform(SocialPlatform.PocketCasts)
            .build()

        client.share(request)
        val clipData = shareStarter.requireShareLink

        assertEquals("https://pca.st/episode/episode-uuid?t=10", clipData.getItemAt(0).text)
        assertEquals(context.getString(LR.string.share_link_bookmark), clipData.description.label)
        assertNull(shareStarter.shareIntent)
    }

    @Test
    fun copyBookmarkLinkWithFeedback() = runTest {
        val client = createClient(showCustomCopyFeedback = true)

        val request = SharingRequest.bookmark(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            position = 10.seconds,
        ).setPlatform(SocialPlatform.PocketCasts)
            .build()

        val response = client.share(request)

        assertEquals(context.getString(LR.string.share_link_copied_feedback), response.feedbackMessage)
    }

    @Test
    fun copyBookmarkLinkWithoutFeedback() = runTest {
        val client = createClient(showCustomCopyFeedback = false)

        val request = SharingRequest.bookmark(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            position = 10.seconds,
        ).setPlatform(SocialPlatform.PocketCasts)
            .build()

        val response = client.share(request)

        assertNull(response.feedbackMessage)
    }

    @Test
    fun shareEpisodeFile() = runTest {
        val file = File(context.cacheDir, "file.mp3").also { it.writeBytes(Random.nextBytes(8)) }
        val request = SharingRequest.episodeFile(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", downloadedFilePath = file.path, fileType = "audio/mp3", publishedDate = Date()),
        ).build()

        val response = client.share(request)
        assertTrue(response.isSuccsessful)
        assertNull(response.feedbackMessage)

        val intent = shareStarter.requireShareIntent

        assertEquals(ACTION_SEND, intent.action)
        assertEquals("audio/mp3", intent.type)
        assertEquals(FileUtil.getUriForFile(context, file), IntentCompat.getParcelableExtra(intent, EXTRA_STREAM, Uri::class.java))
    }

    @Test
    fun shareEpisodeFileWhenFileDoesNotExist() = runTest {
        val request = SharingRequest.episodeFile(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", downloadedFilePath = null, publishedDate = Date()),
        ).build()

        val response = client.share(request)
        assertFalse(response.isSuccsessful)
        assertEquals(context.getString(LR.string.error), response.feedbackMessage)

        assertNull(shareStarter.shareIntent)
    }

    @Test
    fun copyClipLink() = runTest {
        val request = SharingRequest.clipLink(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            range = Clip.Range(15.seconds, 28.seconds),
        ).build()

        client.share(request)
        val clipData = shareStarter.requireShareLink

        assertEquals("https://pca.st/episode/episode-uuid?t=15,28", clipData.getItemAt(0).text)
        assertEquals(context.getString(LR.string.share_link_clip), clipData.description.label)
        assertNull(shareStarter.shareIntent)
    }

    @Test
    fun copyClipLinkWithFeedback() = runTest {
        val client = createClient(showCustomCopyFeedback = true)

        val request = SharingRequest.clipLink(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            range = Clip.Range(15.seconds, 28.seconds),
        ).build()

        val response = client.share(request)

        assertEquals(context.getString(LR.string.share_link_copied_feedback), response.feedbackMessage)
    }

    @Test
    fun copyClipLinkWithoutFeedback() = runTest {
        val client = createClient(showCustomCopyFeedback = false)

        val request = SharingRequest.clipLink(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            range = Clip.Range(15.seconds, 28.seconds),
        ).build()

        val response = client.share(request)

        assertNull(response.feedbackMessage)
    }

    private fun createClient(
        showCustomCopyFeedback: Boolean = false,
    ) = SharingClient(
        context = context,
        tracker = AnalyticsTracker.test(),
        displayPodcastCover = false,
        showCustomCopyFeedback = showCustomCopyFeedback,
        hostUrl = "https://pca.st",
        shareStarter = shareStarter,
    )

    private class TestShareStarter : ShareStarter {
        private var _intent: Intent? = null
        val shareIntent get() = _intent?.let { intent -> IntentCompat.getParcelableExtra(intent, EXTRA_INTENT, Intent::class.java) }
        val requireShareIntent get() = requireNotNull(shareIntent)

        var shareLink: ClipData? = null
            private set
        val requireShareLink get() = requireNotNull(shareLink)

        override fun start(context: Context, intent: Intent) {
            _intent = intent
        }

        override fun copyLink(context: Context, data: ClipData) {
            shareLink = data
        }
    }
}
