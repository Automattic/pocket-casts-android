package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getArtworkUrl
import au.com.shiftyjelly.pocketcasts.repositories.playback.EpisodeFileMetadata
import java.util.Date
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class AutoConverterArtworkTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val podcast = Podcast(uuid = "podcast-uuid")
    private val podcastArtworkUrl = podcast.getArtworkUrl(480)
    private val episodeImageUrl = "https://example.com/episode.jpg"

    @After
    fun tearDown() {
        EpisodeFileMetadata.artworkCacheFile(context, "episode-uuid").delete()
        EpisodeFileMetadata.artworkCacheFile(context, "file-uuid").delete()
    }

    private fun podcastEpisode(imageUrl: String? = null) = PodcastEpisode(
        uuid = "episode-uuid",
        podcastUuid = podcast.uuid,
        publishedDate = Date(),
        imageUrl = imageUrl,
    )

    @Test
    fun `uses episode artwork with the podcast artwork as fallback`() {
        val episode = podcastEpisode(imageUrl = episodeImageUrl)

        val uri = AutoConverter.getPodcastArtworkUri(podcast, episode, context, useEpisodeArtwork = true)!!

        assertEquals(episodeImageUrl, uri.lastPathSegment)
        assertEquals(podcastArtworkUrl, uri.getQueryParameter(ALBUM_ART_FALLBACK_PARAM))
    }

    @Test
    fun `uses podcast artwork without fallback when episode artwork is off`() {
        val episode = podcastEpisode(imageUrl = episodeImageUrl)

        val uri = AutoConverter.getPodcastArtworkUri(podcast, episode, context, useEpisodeArtwork = false)!!

        assertEquals(podcastArtworkUrl, uri.lastPathSegment)
        assertNull(uri.getQueryParameter(ALBUM_ART_FALLBACK_PARAM))
    }

    @Test
    fun `uses podcast artwork when the episode has no artwork`() {
        val episode = podcastEpisode(imageUrl = " ")

        val uri = AutoConverter.getPodcastArtworkUri(podcast, episode, context, useEpisodeArtwork = true)!!

        assertEquals(podcastArtworkUrl, uri.lastPathSegment)
        assertNull(uri.getQueryParameter(ALBUM_ART_FALLBACK_PARAM))
    }

    @Test
    fun `uses embedded file artwork when the episode has no image URL`() {
        val embedded = EpisodeFileMetadata.artworkCacheFile(context, "episode-uuid")
            .apply { writeBytes(byteArrayOf(1)) }
        val episode = podcastEpisode()

        val uri = AutoConverter.getPodcastArtworkUri(podcast, episode, context, useEpisodeArtwork = true)!!

        assertEquals(embedded.path, uri.lastPathSegment)
        assertEquals(podcastArtworkUrl, uri.getQueryParameter(ALBUM_ART_FALLBACK_PARAM))
    }

    @Test
    fun `uses embedded artwork for a file with its own artwork as fallback`() {
        val embedded = EpisodeFileMetadata.artworkCacheFile(context, "file-uuid").apply { writeBytes(byteArrayOf(1)) }
        val fileArtworkUrl = "https://example.com/file.jpg"
        val episode = UserEpisode(uuid = "file-uuid", publishedDate = Date(), artworkUrl = fileArtworkUrl)

        val uri = AutoConverter.getPodcastArtworkUri(null, episode, context, useEpisodeArtwork = true)!!

        assertEquals(embedded.path, uri.lastPathSegment)
        assertEquals(fileArtworkUrl, uri.getQueryParameter(ALBUM_ART_FALLBACK_PARAM))
    }

    @Test
    fun `uses podcast artwork without fallback when there is no episode`() {
        val uri = AutoConverter.getPodcastArtworkUri(podcast, null, context, useEpisodeArtwork = true)!!

        assertEquals(podcastArtworkUrl, uri.lastPathSegment)
        assertNull(uri.getQueryParameter(ALBUM_ART_FALLBACK_PARAM))
    }
}
