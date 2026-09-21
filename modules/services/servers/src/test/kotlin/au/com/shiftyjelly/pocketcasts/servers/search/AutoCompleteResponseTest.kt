package au.com.shiftyjelly.pocketcasts.servers.search

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoCompleteResponseTest {
    private val adapter = Moshi.Builder()
        .add(AutoCompleteResult.jsonAdapter)
        .build()
        .adapter(AutoCompleteResponse::class.java)

    @Test
    fun `unknown result types map to Unknown without failing the whole response`() {
        val response = adapter.fromJson(
            """
            {"results":[
              {"value":"freakonomics","type":"term"},
              {"value":{"uuid":"p1","title":"Freakonomics Radio"},"type":"podcast"},
              {"value":"anything","type":"network"}
            ]}
            """.trimIndent(),
        )

        val types = response?.results?.map { it::class.simpleName }
        assertEquals(listOf("TermResult", "PodcastResult", "Unknown"), types)
    }
}
