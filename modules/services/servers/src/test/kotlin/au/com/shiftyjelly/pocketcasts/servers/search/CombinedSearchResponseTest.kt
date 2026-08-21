package au.com.shiftyjelly.pocketcasts.servers.search

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedSearchResponseTest {
    private val adapter = Moshi.Builder()
        .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
        .add(CombinedResult.jsonAdapter)
        .build()
        .adapter(CombinedSearchResponse::class.java)

    @Test
    fun `unknown result types map to Unknown without failing the whole response`() {
        val response = adapter.fromJson(
            """
            {"results":[
              {"uuid":"p1","title":"Freakonomics Radio","slug":"freakonomics-radio","type":"podcast"},
              {"uuid":"n1","title":"Some Network","type":"network"},
              {"uuid":"p2","title":"Another Show","slug":"another-show","type":"podcast"}
            ]}
            """.trimIndent(),
        )

        val types = response?.results?.map { it::class.simpleName }
        assertEquals(listOf("PodcastResult", "Unknown", "PodcastResult"), types)
    }

    @Test
    fun `registered podcast and episode subtypes decode alongside the fallback`() {
        val response = adapter.fromJson(
            """
            {"results":[
              {"uuid":"p1","title":"Freakonomics Radio","slug":"freakonomics-radio","type":"podcast"},
              {"uuid":"e1","title":"Ep 1","url":"https://example.com/1.mp3","published_date":"2024-01-02T03:04:05Z","podcast_uuid":"p1","podcast_title":"Freakonomics Radio","podcast_slug":"freakonomics-radio","type":"episode"},
              {"uuid":"n1","title":"Some Network","type":"network"}
            ]}
            """.trimIndent(),
        )

        val types = response?.results?.map { it::class.simpleName }
        assertEquals(listOf("PodcastResult", "EpisodeResult", "Unknown"), types)
    }
}
