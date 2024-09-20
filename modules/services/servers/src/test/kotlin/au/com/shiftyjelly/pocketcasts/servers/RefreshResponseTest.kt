package au.com.shiftyjelly.pocketcasts.servers

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshResponseTest {
    @Test
    fun `merge two responses`() {
        val episode1 = PodcastEpisode("episode-id", title = "Episode 1", publishedDate = Date())
        val episode2 = PodcastEpisode("episode-id", title = "Episode 2", publishedDate = Date())

        val response1 = RefreshResponse().apply {
            addUpdate("podcast-1", listOf(episode1))
        }
        val response2 = RefreshResponse().apply {
            addUpdate("podcast-2", listOf(episode2))
        }

        val result = response1.merge(response2)

        val expected = RefreshResponse().apply {
            addUpdate("podcast-1", listOf(episode1))
            addUpdate("podcast-2", listOf(episode2))
        }
        assertEquals(expected, result)
    }
}
