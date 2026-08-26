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
}
