package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import android.content.Context
import au.com.shiftyjelly.pocketcasts.servers.di.NetworkModule
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewAudience
import com.squareup.moshi.Moshi
import java.io.File
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WhatsNewCatalogStoreTest {
    private val moshi: Moshi = NetworkModule().provideMoshi()
    private lateinit var context: Context
    private lateinit var store: WhatsNewCatalogStore

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        File(context.cacheDir, "whats-new").deleteRecursively()
        store = WhatsNewCatalogStore(context, moshi)
    }

    @Test
    fun `nothing is cached before a catalog is written`() {
        assertNull(store.read("en"))
        assertNull(store.writtenAt("en"))
    }

    @Test
    fun `a catalog survives being written and read back`() {
        store.write("en", catalogJson())

        val messages = store.read("en")?.messages
        assertEquals(listOf("Browse by network"), messages?.map { it.title })
    }

    @Test
    fun `a message hidden by audiences this version cannot read stays hidden after being cached`() {
        val audiences = """[{ "key": "plus" }]"""
        val body = catalogJson(audiences = audiences)
        val fetched = requireNotNull(store.decode(body)).messages.single().targeting

        store.write("en", body)
        val cached = store.read("en")?.messages?.single()?.targeting

        assertEquals(false, fetched.targets(WhatsNewAudience.Free))
        assertEquals(fetched.targets(WhatsNewAudience.Free), cached?.targets(WhatsNewAudience.Free))
    }

    @Test
    fun `a catalog is kept apart from the one for another locale`() {
        store.write("en", catalogJson())

        assertNull(store.read("fr"))
    }

    @Test
    fun `writing a catalog records when it was written`() {
        store.write("en", catalogJson())

        val writtenAt = requireNotNull(store.writtenAt("en"))
        assertTrue(Duration.between(writtenAt, Instant.now()).abs() < Duration.ofSeconds(5))
    }

    @Test
    fun `a cached catalog that cannot be read back is treated as missing`() {
        store.write("en", catalogJson())
        File(File(context.cacheDir, "whats-new"), "catalog-en.json").writeText("{ not json")

        assertNull(store.read("en"))
    }

    private fun catalogJson(audiences: String = """["free"]"""): String {
        return """
            {
              "schemaVersion": 1,
              "generatedAt": "2026-09-22T11:39:17Z",
              "platform": "android",
              "locale": "en",
              "messages": [
                {
                  "id": "m1",
                  "type": "new_feature",
                  "publishedAt": "2026-09-18T05:11:26Z",
                  "targeting": { "audiences": $audiences },
                  "title": "Browse by network",
                  "pages": [{ "heading": "h", "description": "d" }]
                }
              ]
            }
        """.trimIndent()
    }
}
