package au.com.shiftyjelly.pocketcasts.servers.podcast

import au.com.shiftyjelly.pocketcasts.models.type.MediaKind
import au.com.shiftyjelly.pocketcasts.models.type.MediaKindMoshiAdapter
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PodcastResponseTest {

    private val adapter = Moshi.Builder()
        .add(MediaKind::class.java, MediaKindMoshiAdapter().nullSafe())
        .build()
        .adapter(PodcastResponse::class.java)

    @Test
    fun `podcast in a network stores the list id`() {
        val podcast = adapter.fromJson(podcastJson(NETWORK_LIST))?.toPodcast()

        assertEquals("cdb75bc0-9f5a-4217-b1ca-f573821a7913", podcast?.networkListId)
    }

    @Test
    fun `podcast without a network list stores null`() {
        val podcast = adapter.fromJson(podcastJson(networkList = null))?.toPodcast()

        assertNull(podcast?.networkListId)
    }

    @Test
    fun `blank list id is stored as null`() {
        val podcast = adapter.fromJson(podcastJson("""{ "list_id": "   " }"""))?.toPodcast()

        assertNull(podcast?.networkListId)
    }

    @Test
    fun `network list without a list id is stored as null`() {
        val podcast = adapter.fromJson(podcastJson("""{ "source": "https://lists.pocketcasts.net/x.json" }"""))?.toPodcast()

        assertNull(podcast?.networkListId)
    }

    private fun podcastJson(networkList: String?) = """
        {
          "episode_frequency": "Monthly",
          "episode_count": 251,
          "has_more_episodes": false,
          "has_seasons": false,
          "season_count": 0,
          "podcast": {
            "uuid": "d041df50-4850-0132-cb49-5f4c86fd3263",
            "title": "Analog(ue)",
            "author": "Relay"
            ${networkList?.let { ""","network_list": $it""" }.orEmpty()}
          }
        }
    """.trimIndent()

    private companion object {
        const val NETWORK_LIST = """
            {
              "list_id": "cdb75bc0-9f5a-4217-b1ca-f573821a7913",
              "source": "https://lists.pocketcasts.net/cdb75bc0-9f5a-4217-b1ca-f573821a7913.json"
            }
        """
    }
}
