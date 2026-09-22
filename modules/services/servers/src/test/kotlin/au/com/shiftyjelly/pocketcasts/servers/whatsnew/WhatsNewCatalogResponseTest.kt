package au.com.shiftyjelly.pocketcasts.servers.whatsnew

import au.com.shiftyjelly.pocketcasts.servers.adapters.LossyListAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import java.time.Instant
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewCatalogResponseTest {
    private val adapter = Moshi.Builder()
        .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
        .add(LossyListAdapterFactory())
        .build()
        .adapter(WhatsNewCatalogResponse::class.java)

    @Test
    fun `decodes the catalog the server publishes`() {
        val catalog = decode(
            """
            {
              "schemaVersion": 1,
              "generatedAt": "2026-09-22T11:39:17Z",
              "platform": "android",
              "locale": "en",
              "messages": [
                {
                  "id": "cdcdcd5a-2ebb-4856-bfc1-529bdec907c4",
                  "type": "new_feature",
                  "publishedAt": "2026-09-18T05:11:26Z",
                  "expiresAt": null,
                  "targeting": { "audiences": ["free", "plus", "patron"], "minimumAppVersion": null },
                  "title": "Browse by network",
                  "pages": [
                    {
                      "image": {
                        "url": "https://static.pocketcasts.net/whats-new/media/cec4f958.webp",
                        "width": 904,
                        "height": 1836,
                        "alt": "Discover Networks screenshot"
                      },
                      "heading": "You already trust the name",
                      "description": "Browse networks in Discover to see every podcast they make."
                    }
                  ]
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, catalog.schemaVersion)
        assertEquals("android", catalog.platform)
        assertEquals("en", catalog.locale)

        val message = catalog.messages.single()
        assertEquals("cdcdcd5a-2ebb-4856-bfc1-529bdec907c4", message.id)
        assertEquals(WhatsNewMessageType.NewFeature, message.type)
        assertEquals(Instant.parse("2026-09-18T05:11:26Z"), message.publishedAt)
        assertNull(message.expiresAt)
        assertEquals(listOf("free", "plus", "patron"), message.targeting.audiences)
        assertNull(message.targeting.minimumAppVersion)
        assertEquals("Browse by network", message.title)

        val page = (message.content as WhatsNewContent.Pages).pages.single()
        assertEquals("You already trust the name", page.heading)
        assertEquals("Discover Networks screenshot", page.image?.alt)
        assertEquals(904f / 1836f, page.image?.aspectRatio)
        assertNull(page.action)
    }

    @Test
    fun `decodes a research poll`() {
        val catalog = decode(
            catalogOf(
                """
                {
                  "id": "m1",
                  "type": "research",
                  "publishedAt": "2026-08-12T09:00:00Z",
                  "targeting": {},
                  "title": "Help shape the player",
                  "description": "It takes one tap.",
                  "poll": {
                    "pollId": "p1",
                    "pollKey": "player_improvements_2026",
                    "question": "What should we improve next?",
                    "options": [
                      { "id": "o1", "pollOptionKey": "up_next", "label": "Up Next" },
                      { "id": "o2", "pollOptionKey": "sleep_timer", "label": "The sleep timer" }
                    ]
                  }
                }
                """.trimIndent(),
            ),
        )

        val research = (catalog.messages.single().content as WhatsNewContent.Research).research
        assertEquals("It takes one tap.", research.description)
        assertEquals("player_improvements_2026", research.poll.pollKey)
        assertEquals(listOf("up_next", "sleep_timer"), research.poll.options.map { it.pollOptionKey })
    }

    @Test
    fun `a message of a type this version does not know is dropped`() {
        val catalog = decode(
            catalogOf(
                """
                {
                  "id": "m1",
                  "type": "video_tour",
                  "publishedAt": "2026-08-12T09:00:00Z",
                  "targeting": {},
                  "title": "Take the tour",
                  "video": { "url": "https://static.pocketcasts.com/tour.mp4" }
                }
                """.trimIndent(),
            ),
        )

        assertTrue(catalog.messages.isEmpty())
    }

    @Test
    fun `a message carrying the other type's content is dropped`() {
        val catalog = decode(
            catalogOf(
                message(type = "tip", body = """"poll": { "pollId": "p1", "pollKey": "k", "question": "q", "options": [{ "id": "o1", "pollOptionKey": "k", "label": "l" }] }"""),
                message(type = "research", body = """"pages": [{ "heading": "h", "description": "d" }]"""),
            ),
        )

        assertTrue(catalog.messages.isEmpty())
    }

    @Test
    fun `a message with no pages is dropped`() {
        val catalog = decode(catalogOf(message(body = """"pages": []""")))

        assertTrue(catalog.messages.isEmpty())
    }

    @Test
    fun `a message with one invalid page is dropped whole`() {
        val catalog = decode(
            catalogOf(
                message(body = """"pages": [{ "heading": "h", "description": "d" }, { "heading": "", "description": "d" }]"""),
            ),
        )

        assertTrue(catalog.messages.isEmpty())
    }

    @Test
    fun `a required field that says nothing drops the message`() {
        val blankFields = listOf(
            message(title = "   "),
            """{ "id": " ", "type": "tip", "publishedAt": "2026-08-12T09:00:00Z", "targeting": {}, "title": "t", "pages": [{ "heading": "h", "description": "d" }] }""",
            message(body = """"pages": [{ "heading": " ", "description": "d" }]"""),
            message(body = """"pages": [{ "heading": "h", "description": " " }]"""),
            message(type = "research", body = """"poll": { "pollId": " ", "pollKey": "k", "question": "q", "options": [{ "id": "o1", "pollOptionKey": "k", "label": "l" }] }"""),
            message(type = "research", body = """"poll": { "pollId": "p1", "pollKey": "k", "question": "q", "options": [{ "id": " ", "pollOptionKey": "k", "label": "l" }] }"""),
        )

        blankFields.forEach { json ->
            assertTrue(json, decode(catalogOf(json)).messages.isEmpty())
        }
    }

    @Test
    fun `a page with no image keeps what it says`() {
        val catalog = decode(catalogOf(message(body = """"pages": [{ "heading": "Read along", "description": "d" }]""")))

        val page = (catalog.messages.single().content as WhatsNewContent.Pages).pages.single()
        assertNull(page.image)
        assertEquals("Read along", page.heading)
    }

    @Test
    fun `an image with no published size has no shape to leave room for`() {
        val catalog = decode(
            catalogOf(message(body = """"pages": [{ "image": { "url": "https://static.pocketcasts.com/a.webp" }, "heading": "h", "description": "d" }]""")),
        )

        val image = (catalog.messages.single().content as WhatsNewContent.Pages).pages.single().image
        assertEquals("https://static.pocketcasts.com/a.webp", image?.url)
        assertNull(image?.aspectRatio)
    }

    @Test
    fun `an image with no alt text is still published`() {
        val catalog = decode(
            catalogOf(message(body = """"pages": [{ "image": { "url": "https://static.pocketcasts.com/a.webp", "alt": "" }, "heading": "h", "description": "d" }]""")),
        )

        val image = (catalog.messages.single().content as WhatsNewContent.Pages).pages.single().image
        assertNull(image?.alt)
    }

    @Test
    fun `an incomplete action drops the message`() {
        val catalog = decode(
            catalogOf(message(body = """"pages": [{ "heading": "h", "description": "d", "action": { "event": "", "label": "Try it" } }]""")),
        )

        assertTrue(catalog.messages.isEmpty())
    }

    @Test
    fun `an action naming an unknown event still decodes`() {
        val catalog = decode(
            catalogOf(message(body = """"pages": [{ "heading": "h", "description": "d", "action": { "event": "open_something_later", "label": "Try it" } }]""")),
        )

        val page = (catalog.messages.single().content as WhatsNewContent.Pages).pages.single()
        assertEquals(WhatsNewAction(event = "open_something_later", label = "Try it"), page.action)
    }

    @Test
    fun `a research message with nothing to answer is dropped`() {
        val catalog = decode(
            catalogOf(
                message(type = "research", body = """"poll": { "pollId": "p1", "pollKey": "k", "question": "q", "options": [] }"""),
            ),
        )

        assertTrue(catalog.messages.isEmpty())
    }

    @Test
    fun `fields this version does not know are ignored`() {
        val catalog = decode(
            """
            {
              "schemaVersion": 2,
              "publishedChannel": "beta",
              "messages": [
                {
                  "id": "m1",
                  "type": "tip",
                  "publishedAt": "2026-08-12T09:00:00Z",
                  "targeting": { "audiences": ["free"], "platforms": ["android"] },
                  "title": "Sort your Up Next",
                  "pages": [{ "heading": "h", "description": "d", "footnote": "later" }]
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("Sort your Up Next", catalog.messages.single().title)
    }

    @Test
    fun `decodes timestamps with fractional seconds`() {
        val catalog = decode(catalogOf(message(publishedAt = "2026-08-12T09:00:00.123Z")))

        assertEquals(Instant.parse("2026-08-12T09:00:00.123Z"), catalog.messages.single().publishedAt)
    }

    @Test
    fun `an expiry the server published is kept`() {
        val catalog = decode(catalogOf(message(expiresAt = "2026-09-12T09:00:00Z")))

        assertEquals(Instant.parse("2026-09-12T09:00:00Z"), catalog.messages.single().expiresAt)
    }

    @Test
    fun `a message with no audiences targets everyone`() {
        val catalog = decode(catalogOf(message()))

        val targeting = catalog.messages.single().targeting
        assertTrue(WhatsNewAudience.entries.all(targeting::targets))
    }

    @Test
    fun `a message that cannot be decoded is dropped without losing its neighbours`() {
        val catalog = decode(
            catalogOf(
                message(title = "Sort your Up Next"),
                """{ "id": "m2", "type": "tip", "publishedAt": "not a date", "targeting": {}, "title": "Broken", "pages": [{ "heading": "h", "description": "d" }] }""",
                message(title = "Browse by network"),
            ),
        )

        assertEquals(listOf("Sort your Up Next", "Browse by network"), catalog.messages.map { it.title })
    }

    @Test
    fun `a message whose content is the wrong shape is dropped without losing its neighbours`() {
        val catalog = decode(
            catalogOf(
                message(title = "Sort your Up Next"),
                """{ "id": "m2", "type": "tip", "publishedAt": "2026-08-12T09:00:00Z", "targeting": {}, "title": "Broken", "pages": "soon" }""",
            ),
        )

        assertEquals(listOf("Sort your Up Next"), catalog.messages.map { it.title })
    }

    @Test
    fun `an audience this version cannot read is dropped without losing its neighbours`() {
        val catalog = decode(
            catalogOf(message(targeting = """{ "audiences": ["free", { "tier": "beta" }, "patron"] }""")),
        )

        assertEquals(listOf("free", "patron"), catalog.messages.single().targeting.audiences)
    }

    @Test
    fun `a message whose audiences are all unreadable targets nobody rather than everyone`() {
        val catalog = decode(catalogOf(message(targeting = """{ "audiences": [{ "tier": "plus" }] }""")))

        val targeting = catalog.messages.single().targeting
        assertTrue(WhatsNewAudience.entries.none(targeting::targets))
    }

    @Test
    fun `a timestamp the catalog itself cannot be read by does not cost it its messages`() {
        val catalog = decode(
            """{ "schemaVersion": null, "generatedAt": "2026-09-22 11:39:17", "messages": [${message(title = "Browse by network")}] }""",
        )

        assertNull(catalog.generatedAt)
        assertEquals(listOf("Browse by network"), catalog.messages.map { it.title })
    }

    @Test
    fun `a message that repeats a field keeps the last one it was sent`() {
        val catalog = decode(
            catalogOf(
                """{ "id": "m1", "id": "m2", "type": "tip", "publishedAt": "2026-08-12T09:00:00Z", "targeting": {}, "title": "Repeated", "pages": [{ "heading": "h", "description": "d" }] }""",
                message(title = "Browse by network"),
            ),
        )

        assertEquals(listOf("Repeated", "Browse by network"), catalog.messages.map { it.title })
        assertEquals("m2", catalog.messages.first().id)
    }

    @Test
    fun `a message with a blank id is dropped`() {
        val catalog = decode(
            catalogOf("""{ "id": "  ", "type": "tip", "publishedAt": "2026-08-12T09:00:00Z", "targeting": {}, "title": "t", "pages": [{ "heading": "h", "description": "d" }] }"""),
        )

        assertTrue(catalog.messages.isEmpty())
    }

    @Test
    fun `a message aimed only at an audience this version does not know targets nobody`() {
        val catalog = decode(
            catalogOf(message(targeting = """{ "audiences": ["future_audience"] }""")),
        )

        val targeting = catalog.messages.single().targeting
        assertTrue(WhatsNewAudience.entries.none(targeting::targets))
    }

    @Test
    fun `a list the server sends as null is read as an empty one`() {
        val catalog = decode("""{ "schemaVersion": 1, "messages": null }""")

        assertTrue(catalog.messages.isEmpty())
    }

    @Test
    fun `audiences the server sends as null target everyone`() {
        val catalog = decode(catalogOf(message(targeting = """{ "audiences": null }""")))

        val targeting = catalog.messages.single().targeting
        assertTrue(WhatsNewAudience.entries.all(targeting::targets))
    }

    private fun decode(json: String) = requireNotNull(adapter.fromJson(json)).toCatalog()

    private fun catalogOf(vararg messages: String) = """{ "schemaVersion": 1, "messages": [${messages.joinToString(",")}] }"""

    private fun message(
        type: String = "tip",
        title: String = "Sort your Up Next",
        publishedAt: String = "2026-08-12T09:00:00Z",
        expiresAt: String? = null,
        targeting: String = "{}",
        body: String = """"pages": [{ "heading": "h", "description": "d" }]""",
    ) = """
        {
          "id": "m1",
          "type": "$type",
          "publishedAt": "$publishedAt",
          "expiresAt": ${expiresAt?.let { "\"$it\"" }},
          "targeting": $targeting,
          "title": "$title",
          $body
        }
    """.trimIndent()
}
