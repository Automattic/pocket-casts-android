package au.com.shiftyjelly.pocketcasts.servers.model

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverModelTest {

    private val adapter = Moshi.Builder()
        .add(ListTypeMoshiAdapter())
        .add(DisplayStyleMoshiAdapter())
        .add(ExpandedStyleMoshiAdapter())
        .build()
        .adapter(DiscoverRow::class.java)

    @Test
    fun `discover row defaults source and expanded style when the keys are absent`() {
        val row = adapter.fromJson(
            """
            {
              "type": "podcast_list",
              "summary_style": "small_list",
              "title": "Trending",
              "regions": ["us"]
            }
            """.trimIndent(),
        )

        assertEquals("", row?.source)
        assertTrue(row?.expandedStyle is ExpandedStyle.PlainList)
    }

    @Test
    fun `discover row parses the networks lists list row`() {
        val row = adapter.fromJson(
            """
            {
              "id": "0b370140-ae34-4f10-9b6f-301820de0605",
              "uuid": "networks",
              "title": "Networks",
              "type": "lists_list",
              "summary_style": "large_list",
              "expanded_style": "network_grid",
              "curated": true,
              "source": "https://lists.pocketcasts.net/0b370140-ae34-4f10-9b6f-301820de0605.json",
              "category_id": null,
              "authenticated": false,
              "regions": ["us"]
            }
            """.trimIndent(),
        )

        assertTrue(row?.type is ListType.ListsList)
        assertTrue(row?.displayStyle is DisplayStyle.LargeList)
        assertTrue(row?.expandedStyle is ExpandedStyle.NetworkGrid)
    }

    @Test
    fun `discover row keeps an unrecognised type as unknown`() {
        val row = adapter.fromJson(
            """
            {
              "type": "video_list",
              "summary_style": "small_list",
              "title": "Watch",
              "regions": ["us"]
            }
            """.trimIndent(),
        )

        assertEquals(ListType.Unknown("video_list"), row?.type)
    }

    @Test
    fun `discover row parses the video preview list summary style`() {
        val row = adapter.fromJson(
            """
            {
              "type": "episode_list",
              "summary_style": "video_preview_list",
              "expanded_style": "plain_list",
              "title": "Made for TV",
              "regions": ["us"]
            }
            """.trimIndent(),
        )

        assertTrue(row?.displayStyle is DisplayStyle.VideoPreviewList)
    }
}
