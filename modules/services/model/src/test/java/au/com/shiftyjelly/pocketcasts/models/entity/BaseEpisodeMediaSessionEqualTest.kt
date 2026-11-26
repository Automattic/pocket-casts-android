package au.com.shiftyjelly.pocketcasts.models.entity

import java.util.Date
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseEpisodeMediaSessionEqualTest {

    private val publishedDate = Date()

    @Test
    fun `same podcast episode is equal`() {
        val episode = PodcastEpisode(
            uuid = "episode",
            publishedDate = publishedDate,
            imageUrl = "https://example.com/a.jpg",
        )

        assertTrue(BaseEpisode.isMediaSessionEqual(episode, episode.copy()))
    }

    @Test
    fun `podcast episode differs once its image URL arrives`() {
        val withoutImage = PodcastEpisode(uuid = "episode", publishedDate = publishedDate)
        val withImage = withoutImage.copy(imageUrl = "https://example.com/a.jpg")

        assertFalse(BaseEpisode.isMediaSessionEqual(withoutImage, withImage))
    }

    @Test
    fun `podcast episode differs when starred changes`() {
        val episode = PodcastEpisode(uuid = "episode", publishedDate = publishedDate)

        assertFalse(BaseEpisode.isMediaSessionEqual(episode, episode.copy(isStarred = true)))
    }

    @Test
    fun `different episodes are not equal`() {
        val episode = PodcastEpisode(uuid = "episode", publishedDate = publishedDate)

        assertFalse(BaseEpisode.isMediaSessionEqual(episode, episode.copy(uuid = "other")))
    }

    @Test
    fun `missing episodes are not equal`() {
        val episode = PodcastEpisode(uuid = "episode", publishedDate = publishedDate)

        assertFalse(BaseEpisode.isMediaSessionEqual(episode, null))
        assertFalse(BaseEpisode.isMediaSessionEqual(null, episode))
        assertFalse(BaseEpisode.isMediaSessionEqual(null, null))
    }
}
