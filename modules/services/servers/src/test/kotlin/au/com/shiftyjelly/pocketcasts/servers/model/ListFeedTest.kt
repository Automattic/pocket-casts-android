package au.com.shiftyjelly.pocketcasts.servers.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListFeedTest {

    private val adapter = Moshi.Builder()
        .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
        .add(ListTypeMoshiAdapter())
        .add(DisplayStyleMoshiAdapter())
        .add(ExpandedStyleMoshiAdapter())
        .build()
        .adapter(ListFeed::class.java)

    @Test
    fun `networks feed describes itself and decodes its entries`() {
        val feed = adapter.fromJson(NETWORKS_FEED)

        assertTrue(feed?.type is ListType.ListsList)
        assertTrue(feed?.summaryStyle is DisplayStyle.LargeList)
        assertTrue(feed?.expandedStyle is ExpandedStyle.NetworkGrid)
        assertEquals("0b370140-ae34-4f10-9b6f-301820de0605", feed?.listId)
        assertEquals(listOf("Relay", "The New York Times"), feed?.networks?.map { it.title })

        val relay = feed?.networks?.first()
        assertEquals("cdb75bc0-9f5a-4217-b1ca-f573821a7913", relay?.uuid)
        assertEquals("https://lists.pocketcasts.net/cdb75bc0-9f5a-4217-b1ca-f573821a7913.json", relay?.source)
        assertEquals("https://static.pocketcasts.net/share/images/cdb75bc0-9f5a-4217-b1ca-f573821a7913-author.png", relay?.collectionImage)
        assertEquals(4, relay?.itemCount)
        assertEquals("relay-network", relay?.urlPath)
        assertEquals("The Relay network of podcasts.", relay?.description)
        assertTrue(relay?.type is ListType.PodcastList)
        assertTrue(relay?.summaryStyle is DisplayStyle.CollectionList)
        assertTrue(relay?.expandedStyle is ExpandedStyle.NetworkGrid)
    }

    @Test
    fun `entries that are not podcast lists are dropped without failing the feed`() {
        val feed = adapter.fromJson(
            """
            {
              "title": "Networks",
              "type": "lists_list",
              "lists": [
                {"uuid": "podcasts", "title": "Relay", "type": "podcast_list"},
                {"uuid": "episodes", "title": "Latest", "type": "episode_list"},
                {"uuid": "future", "title": "Something New", "type": "video_list"}
              ]
            }
            """.trimIndent(),
        )

        assertEquals(listOf("podcasts"), feed?.networks?.map { it.uuid })
        assertEquals(3, feed?.lists?.size)
    }

    @Test
    fun `a feed without a lists array decodes to no entries`() {
        val feed = adapter.fromJson("""{"title": "Featured", "type": "podcast_list"}""")

        assertEquals(emptyList<NetworkListSummary>(), feed?.networks)
    }

    companion object {
        // The staging payload from https://lists.pocketcasts.net/0b370140-ae34-4f10-9b6f-301820de0605.json, trimmed to two entries.
        private val NETWORKS_FEED = """
            {
              "title": "Networks",
              "description": "Podcast Networks",
              "short_description": "",
              "author": null,
              "datetime": "2026-09-03T05:27:38Z",
              "list_id": "0b370140-ae34-4f10-9b6f-301820de0605",
              "type": "lists_list",
              "summary_style": "large_list",
              "expanded_style": "network_grid",
              "collection_image": null,
              "colors": {"onLightBackground": "", "onDarkBackground": ""},
              "subtitle": "",
              "web_url": "",
              "web_title": "",
              "collage_images": [],
              "podcasts": [],
              "lists": [
                {
                  "uuid": "cdb75bc0-9f5a-4217-b1ca-f573821a7913",
                  "title": "Relay",
                  "type": "podcast_list",
                  "summary_style": "collection",
                  "expanded_style": "network_grid",
                  "source": "https://lists.pocketcasts.net/cdb75bc0-9f5a-4217-b1ca-f573821a7913.json",
                  "collection_image": "https://static.pocketcasts.net/share/images/cdb75bc0-9f5a-4217-b1ca-f573821a7913-author.png",
                  "item_count": 4,
                  "description": "The Relay network of podcasts.",
                  "url_path": "relay-network"
                },
                {
                  "uuid": "e07f76d0-0064-48d9-81a3-895de009f5c7",
                  "title": "The New York Times",
                  "type": "podcast_list",
                  "summary_style": "collection",
                  "expanded_style": "network_grid",
                  "source": "https://lists.pocketcasts.net/e07f76d0-0064-48d9-81a3-895de009f5c7.json",
                  "collection_image": "https://static.pocketcasts.net/share/images/e07f76d0-0064-48d9-81a3-895de009f5c7-author.svg",
                  "item_count": 8,
                  "description": "The New York Times collection of podcasts",
                  "url_path": "nytimes-network"
                }
              ]
            }
        """.trimIndent()
    }
}
