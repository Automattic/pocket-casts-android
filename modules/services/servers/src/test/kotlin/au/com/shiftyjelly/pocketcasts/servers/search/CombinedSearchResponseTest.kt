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
              {"uuid":"c1","title":"Some Curated List","type":"curated_list"},
              {"uuid":"p2","title":"Another Show","slug":"another-show","type":"podcast"}
            ]}
            """.trimIndent(),
        )

        val types = response?.results?.map { it::class.simpleName }
        assertEquals(listOf("PodcastResult", "Unknown", "PodcastResult"), types)
    }

    @Test
    fun `registered podcast, episode and network subtypes decode alongside the fallback`() {
        val response = adapter.fromJson(
            """
            {"results":[
              {"uuid":"p1","title":"Freakonomics Radio","slug":"freakonomics-radio","type":"podcast"},
              {"uuid":"e1","title":"Ep 1","url":"https://example.com/1.mp3","published_date":"2024-01-02T03:04:05Z","podcast_uuid":"p1","podcast_title":"Freakonomics Radio","podcast_slug":"freakonomics-radio","type":"episode"},
              {"uuid":"n1","title":"Some Network","type":"network"},
              {"uuid":"c1","title":"Some Curated List","type":"curated_list"}
            ]}
            """.trimIndent(),
        )

        val types = response?.results?.map { it::class.simpleName }
        assertEquals(listOf("PodcastResult", "EpisodeResult", "NetworkResult", "Unknown"), types)
    }

    @Test
    fun `network results decode every field the server sends`() {
        val response = adapter.fromJson(
            """
            {"results":[
              {"uuid":"c73d120f-c174-4324-b0a3-18f9b239a59d","title":"WNYC","short_description":"New York's flagship public radio station","collection_image":"https://static.pocketcasts.com/wnyc-author.png","podcast_count":11,"type":"network"}
            ]}
            """.trimIndent(),
        )

        assertEquals(
            CombinedResult.NetworkResult(
                uuid = "c73d120f-c174-4324-b0a3-18f9b239a59d",
                title = "WNYC",
                shortDescription = "New York's flagship public radio station",
                collectionImage = "https://static.pocketcasts.com/wnyc-author.png",
            ),
            response?.results?.single(),
        )
    }

    @Test
    fun `a network missing its uuid does not fail the whole response`() {
        val response = adapter.fromJson(
            """
            {"results":[
              {"uuid":"p1","title":"Freakonomics Radio","slug":"freakonomics-radio","type":"podcast"},
              {"title":"WNYC","type":"network"}
            ]}
            """.trimIndent(),
        )

        val types = response?.results?.map { it::class.simpleName }
        assertEquals(listOf("PodcastResult", "NetworkResult"), types)
    }

    @Test
    fun `network results decode when the optional fields are missing`() {
        val response = adapter.fromJson(
            """
            {"results":[
              {"uuid":"n1","title":"WNYC","type":"network"}
            ]}
            """.trimIndent(),
        )

        assertEquals(
            CombinedResult.NetworkResult(uuid = "n1", title = "WNYC"),
            response?.results?.single(),
        )
    }

    @Test
    fun `explicit null on unread fields decodes without failing the whole response`() {
        val response = adapter.fromJson(
            """
            {"results":[
              {"uuid":"p1","title":"Freakonomics Radio","slug":null,"type":"podcast"},
              {"uuid":"e1","title":"Ep 1","url":null,"published_date":"2024-01-02T03:04:05Z","podcast_uuid":"p1","podcast_title":"Freakonomics Radio","podcast_slug":null,"type":"episode"}
            ]}
            """.trimIndent(),
        )

        val types = response?.results?.map { it::class.simpleName }
        assertEquals(listOf("PodcastResult", "EpisodeResult"), types)
    }
}
