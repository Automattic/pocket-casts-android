package au.com.shiftyjelly.pocketcasts.sharing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_INTENT
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.EXTRA_TITLE
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.BuildConfig.WEB_BASE_HOST
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import java.io.File
import java.util.Date
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
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

    private val testMediaService = TestMediaService()

    private val client = createClient()

    private val regularPlatforms = SocialPlatform.entries - SocialPlatform.Instagram - SocialPlatform.PocketCasts

    @Test
    fun sharePodcastToRegularPlatforms() = runTest {
        regularPlatforms.forEach { platform ->
            val request = SharingRequest.podcast(Podcast(uuid = "podcast-uuid", title = "Podcast Title"))
                .setPlatform(platform)
                .build()

            client.share(request)
            val intent = shareStarter.requireChooserIntent

            assertEquals(ACTION_SEND, intent.action)
            assertEquals("text/plain", intent.type)
            assertEquals("https://pca.st/podcast/podcast-uuid", intent.getStringExtra(EXTRA_TEXT))
            assertEquals("Podcast Title", intent.getStringExtra(EXTRA_TITLE))
            assertEquals(platform.packageId, intent.`package`)
            assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    @Test
    fun sharePodcastToInstagram() = runTest {
        val file = File(context.cacheDir, "file.mp3").also { it.writeBytes(Random.nextBytes(8)) }
        val request = SharingRequest.podcast(Podcast(uuid = "podcast-uuid", title = "Podcast Title"))
            .setPlatform(SocialPlatform.Instagram)
            .setBackgroundImage(file)
            .build()

        client.share(request)
        val intent = shareStarter.requireIntent

        assertEquals("com.instagram.share.ADD_TO_STORY", intent.action)
        assertEquals("Meta ID", intent.getStringExtra("source_application"))
        assertEquals("image/png", intent.type)
        assertEquals(FileUtil.getUriForFile(context, file), intent.data)
        assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
        assertEquals(FLAG_ACTIVITY_NEW_TASK, intent.flags and FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun failToSharePodcastToInstagramWithoutBackgroundFile() = runTest {
        val request = SharingRequest.podcast(Podcast(uuid = "podcast-uuid", title = "Podcast Title"))
            .setPlatform(SocialPlatform.Instagram)
            .build()

        val response = client.share(request)

        assertNull(shareStarter.intent)
        assertFalse(response.isSuccsessful)
        assertEquals(context.getString(LR.string.share_error_message), response.feedbackMessage)
        assertEquals("Sharing to Instagram requires a background image", response.error?.message)
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
        assertNull(shareStarter.chooserIntent)
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
            val intent = shareStarter.requireChooserIntent

            assertEquals(ACTION_SEND, intent.action)
            assertEquals("text/plain", intent.type)
            assertEquals("https://pca.st/episode/episode-uuid", intent.getStringExtra(EXTRA_TEXT))
            assertEquals("Episode Title", intent.getStringExtra(EXTRA_TITLE))
            assertEquals(platform.packageId, intent.`package`)
            assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    @Test
    fun shareEpisodeToInstagram() = runTest {
        val file = File(context.cacheDir, "file.mp3").also { it.writeBytes(Random.nextBytes(8)) }
        val request = SharingRequest.episode(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
        ).setPlatform(SocialPlatform.Instagram)
            .setBackgroundImage(file)
            .build()

        client.share(request)
        val intent = shareStarter.requireIntent

        assertEquals("com.instagram.share.ADD_TO_STORY", intent.action)
        assertEquals("Meta ID", intent.getStringExtra("source_application"))
        assertEquals("image/png", intent.type)
        assertEquals(FileUtil.getUriForFile(context, file), intent.data)
        assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
        assertEquals(FLAG_ACTIVITY_NEW_TASK, intent.flags and FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun failToShareEpisodeToInstagramWithoutBackgroundFile() = runTest {
        val request = SharingRequest.episode(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
        ).setPlatform(SocialPlatform.Instagram)
            .build()

        val response = client.share(request)

        assertNull(shareStarter.intent)
        assertFalse(response.isSuccsessful)
        assertEquals(context.getString(LR.string.share_error_message), response.feedbackMessage)
        assertEquals("Sharing to Instagram requires a background image", response.error?.message)
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
        assertNull(shareStarter.chooserIntent)
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
                position = 10.seconds + 300.milliseconds,
            ).setPlatform(platform)
                .build()

            client.share(request)
            val intent = shareStarter.requireChooserIntent

            assertEquals(ACTION_SEND, intent.action)
            assertEquals("text/plain", intent.type)
            assertEquals("https://pca.st/episode/episode-uuid?t=10", intent.getStringExtra(EXTRA_TEXT))
            assertEquals("Episode Title", intent.getStringExtra(EXTRA_TITLE))
            assertEquals(platform.packageId, intent.`package`)
            assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    @Test
    fun shareEpisodePositionToInstagram() = runTest {
        val file = File(context.cacheDir, "file.mp3").also { it.writeBytes(Random.nextBytes(8)) }
        val request = SharingRequest.episodePosition(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            position = 10.seconds,
        ).setPlatform(SocialPlatform.Instagram)
            .setBackgroundImage(file)
            .build()

        client.share(request)
        val intent = shareStarter.requireIntent

        assertEquals("com.instagram.share.ADD_TO_STORY", intent.action)
        assertEquals("Meta ID", intent.getStringExtra("source_application"))
        assertEquals("image/png", intent.type)
        assertEquals(FileUtil.getUriForFile(context, file), intent.data)
        assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
        assertEquals(FLAG_ACTIVITY_NEW_TASK, intent.flags and FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun failToShareEpisodePositionToInstagramWithoutBackgroundFile() = runTest {
        val request = SharingRequest.episodePosition(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            position = 10.seconds,
        ).setPlatform(SocialPlatform.Instagram)
            .build()

        val response = client.share(request)

        assertNull(shareStarter.intent)
        assertFalse(response.isSuccsessful)
        assertEquals(context.getString(LR.string.share_error_message), response.feedbackMessage)
        assertEquals("Sharing to Instagram requires a background image", response.error?.message)
    }

    @Test
    fun copyEpisodePositionLink() = runTest {
        val request = SharingRequest.episodePosition(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            position = 10.seconds + 421.milliseconds,
        ).setPlatform(SocialPlatform.PocketCasts)
            .build()

        client.share(request)
        val clipData = shareStarter.requireShareLink

        assertEquals("https://pca.st/episode/episode-uuid?t=10", clipData.getItemAt(0).text)
        assertEquals(context.getString(LR.string.share_link_episode_position), clipData.description.label)
        assertNull(shareStarter.chooserIntent)
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
                position = 10.seconds + 112.milliseconds,
            ).setPlatform(platform)
                .build()

            client.share(request)
            val intent = shareStarter.requireChooserIntent

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
            position = 10.seconds + 677.milliseconds,
        ).setPlatform(SocialPlatform.PocketCasts)
            .build()

        client.share(request)
        val clipData = shareStarter.requireShareLink

        assertEquals("https://pca.st/episode/episode-uuid?t=10", clipData.getItemAt(0).text)
        assertEquals(context.getString(LR.string.share_link_bookmark), clipData.description.label)
        assertNull(shareStarter.chooserIntent)
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

        val intent = shareStarter.requireChooserIntent

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
        assertEquals(context.getString(LR.string.share_error_message), response.feedbackMessage)

        assertNull(shareStarter.chooserIntent)
    }

    @Test
    fun copyClipLink() = runTest {
        val request = SharingRequest.clipLink(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            range = Clip.Range(15.seconds + 200.milliseconds, 28.seconds + 105.milliseconds),
        ).build()

        client.share(request)
        val clipData = shareStarter.requireShareLink

        assertEquals("https://pca.st/episode/episode-uuid?t=15.2,28.1", clipData.getItemAt(0).text)
        assertEquals(context.getString(LR.string.share_link_clip), clipData.description.label)
        assertNull(shareStarter.chooserIntent)
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

    @Test
    fun shareAudioClipToRegularPlatforms() = runTest {
        val file = File(context.cacheDir, "file.mp3").also { it.writeBytes(Random.nextBytes(8)) }
        testMediaService.audioClip = file

        regularPlatforms.forEach { platform ->
            val request = SharingRequest.audioClip(
                podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
                episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
                range = Clip.Range(15.seconds, 28.seconds),
            ).setPlatform(platform)
                .build()

            val response = client.share(request)
            assertTrue(response.isSuccsessful)
            assertNull(response.feedbackMessage)

            val intent = shareStarter.requireChooserIntent

            assertEquals(ACTION_SEND, intent.action)
            assertEquals("audio/mp3", intent.type)
            assertEquals(platform.packageId, intent.`package`)
            assertEquals(FileUtil.getUriForFile(context, file), IntentCompat.getParcelableExtra(intent, EXTRA_STREAM, Uri::class.java))
        }
    }

    @Test
    fun shareAudioClipToPocketCastsAsMore() = runTest {
        val file = File(context.cacheDir, "file.mp3").also { it.writeBytes(Random.nextBytes(8)) }
        testMediaService.audioClip = file

        val request = SharingRequest.audioClip(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            range = Clip.Range(15.seconds, 28.seconds),
        ).setPlatform(SocialPlatform.PocketCasts)
            .build()

        val response = client.share(request)
        assertTrue(response.isSuccsessful)
        assertNull(response.feedbackMessage)

        val intent = shareStarter.requireChooserIntent

        assertEquals(ACTION_SEND, intent.action)
        assertEquals("audio/mp3", intent.type)
        assertNull(intent.`package`)
        assertEquals(FileUtil.getUriForFile(context, file), IntentCompat.getParcelableExtra(intent, EXTRA_STREAM, Uri::class.java))
    }

    @Test
    fun failToShareAudioClip() = runTest {
        testMediaService.audioClip = null

        val request = SharingRequest.audioClip(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            range = Clip.Range(15.seconds, 28.seconds),
        ).build()

        val response = client.share(request)
        assertFalse(response.isSuccsessful)
        assertEquals(context.getString(LR.string.share_error_message), response.feedbackMessage)

        assertNull(shareStarter.chooserIntent)
    }

    @Test
    fun shareVideoClipToRegularPlatforms() = runTest {
        val file = File(context.cacheDir, "file.mp4").also { it.writeBytes(Random.nextBytes(8)) }
        testMediaService.videoClip = file

        regularPlatforms.forEach { platform ->
            val request = SharingRequest.videoClip(
                podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
                episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
                range = Clip.Range(15.seconds, 28.seconds),
                cardType = CardType.Square,
                backgroundImage = File(context.cacheDir, "image.png"),
            ).setPlatform(platform)
                .build()

            val response = client.share(request)
            assertTrue(response.isSuccsessful)
            assertNull(response.feedbackMessage)

            val intent = shareStarter.requireChooserIntent

            assertEquals(ACTION_SEND, intent.action)
            assertEquals("video/mp4", intent.type)
            assertEquals(platform.packageId, intent.`package`)
            assertEquals(FileUtil.getUriForFile(context, file), IntentCompat.getParcelableExtra(intent, EXTRA_STREAM, Uri::class.java))
        }
    }

    @Test
    fun shareVideoClipToInstagram() = runTest {
        val file = File(context.cacheDir, "file.mp4").also { it.writeBytes(Random.nextBytes(8)) }
        testMediaService.videoClip = file

        val request = SharingRequest.videoClip(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            range = Clip.Range(15.seconds, 28.seconds),
            cardType = CardType.Square,
            backgroundImage = File(context.cacheDir, "image.png"),
        ).setPlatform(SocialPlatform.Instagram)
            .build()

        client.share(request)
        val intent = shareStarter.requireIntent

        assertEquals("com.instagram.share.ADD_TO_STORY", intent.action)
        assertEquals("Meta ID", intent.getStringExtra("source_application"))
        assertEquals("video/mp4", intent.type)
        assertEquals(FileUtil.getUriForFile(context, file), intent.data)
        assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
        assertEquals(FLAG_ACTIVITY_NEW_TASK, intent.flags and FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun shareVideoClipToPocketCastsAsMore() = runTest {
        val file = File(context.cacheDir, "file.mp4").also { it.writeBytes(Random.nextBytes(8)) }
        testMediaService.videoClip = file

        val request = SharingRequest.videoClip(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            range = Clip.Range(15.seconds, 28.seconds),
            cardType = CardType.Square,
            backgroundImage = File(context.cacheDir, "image.png"),
        ).setPlatform(SocialPlatform.PocketCasts)
            .build()

        val response = client.share(request)
        assertTrue(response.isSuccsessful)
        assertNull(response.feedbackMessage)

        val intent = shareStarter.requireChooserIntent

        assertEquals(ACTION_SEND, intent.action)
        assertEquals("video/mp4", intent.type)
        assertNull(intent.`package`)
        assertEquals(FileUtil.getUriForFile(context, file), IntentCompat.getParcelableExtra(intent, EXTRA_STREAM, Uri::class.java))
    }

    @Test
    fun failToShareVideoClip() = runTest {
        testMediaService.videoClip = null

        val request = SharingRequest.videoClip(
            podcast = Podcast(uuid = "podcast-uuid", title = "Podcast Title"),
            episode = PodcastEpisode(uuid = "episode-uuid", title = "Episode Title", publishedDate = Date()),
            range = Clip.Range(15.seconds, 28.seconds),
            cardType = CardType.Square,
            backgroundImage = File(context.cacheDir, "image.png"),
        ).build()

        val response = client.share(request)
        assertFalse(response.isSuccsessful)
        assertEquals(context.getString(LR.string.share_error_message), response.feedbackMessage)

        assertNull(shareStarter.chooserIntent)
    }

    @Test
    fun shareReferralLink() = runTest {
        val referralCode = "referral-code"
        val text = context.getString(LR.string.referrals_share_text)
        val subject = context.getString(LR.string.referrals_share_subject)
        val request = SharingRequest.referralLink(
            referralCode = referralCode,
        ).build()

        val response = client.share(request)
        assertTrue(response.isSuccsessful)
        assertNull(response.feedbackMessage)

        val intent = shareStarter.requireChooserIntent

        assertEquals(ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("$text\n\nhttps://$WEB_BASE_HOST/redeem/$referralCode", intent.getStringExtra(EXTRA_TEXT))
        assertEquals(subject, intent.getStringExtra(EXTRA_SUBJECT))
        assertEquals(FLAG_GRANT_READ_URI_PERMISSION, intent.flags and FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun createClient(
        showCustomCopyFeedback: Boolean = false,
    ) = SharingClient(
        context = context,
        mediaService = testMediaService,
        listeners = emptySet(),
        displayPodcastCover = false,
        showCustomCopyFeedback = showCustomCopyFeedback,
        hostUrl = "https://pca.st",
        metaAppId = "Meta ID",
        shareStarter = shareStarter,
    )

    private class TestShareStarter : ShareStarter {
        var intent: Intent? = null
            private set
        val requireIntent get() = requireNotNull(intent)
        val chooserIntent get() = intent?.let { intent -> IntentCompat.getParcelableExtra(intent, EXTRA_INTENT, Intent::class.java) }
        val requireChooserIntent get() = requireNotNull(chooserIntent)

        var shareLink: ClipData? = null
            private set
        val requireShareLink get() = requireNotNull(shareLink)

        override fun start(context: Context, intent: Intent) {
            this.intent = intent
        }

        override fun copyLink(context: Context, data: ClipData) {
            shareLink = data
        }
    }

    private class TestMediaService : MediaService {
        var audioClip: File? = null
        var videoClip: File? = null

        override suspend fun clipAudio(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range): Result<File> = runCatching {
            requireNotNull(audioClip)
        }

        override suspend fun clipVideo(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range, cardType: VisualCardType, backgroundFile: File) = runCatching {
            requireNotNull(videoClip)
        }
    }
}
