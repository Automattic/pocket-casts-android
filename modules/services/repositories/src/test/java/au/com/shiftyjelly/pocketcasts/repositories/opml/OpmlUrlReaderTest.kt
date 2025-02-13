package au.com.shiftyjelly.pocketcasts.repositories.opml

import okio.Buffer
import okio.Source
import org.junit.Assert.assertEquals
import org.junit.Test

class OpmlUrlReaderTest {
    private val reader = OpmlUrlReader()

    @Test
    fun `valid OPML file`() {
        val input = """
            |<?xml version="1.0" encoding="utf-8" standalone="no"?>
            |<opml version="1.0">
            |  <head>
            |    <title>Pocket Casts Feeds</title>
            |  </head>
            |  <body>
            |    <outline text="feeds">
            |      <outline xmlUrl="https://feeds.simplecast.com/1" />
            |      <outline xmlUrl="https://feeds.megaphone.fm/1" text="Title 1" />
            |      <outline xmlUrl="https://feeds.acast.com/public/shows/1" type="rss" />
            |      <outline xmlUrl="https://feeds.acast.com/public/shows/2?key1=value1&key2=value2" text="Title 2" type="rss" />
            |    </outline>
            |  </body>
            |</opml>
        """.trimMargin().asSource()

        val urls = reader.readUrls(input)

        assertEquals(
            listOf(
                "https://feeds.simplecast.com/1",
                "https://feeds.megaphone.fm/1",
                "https://feeds.acast.com/public/shows/1",
                "https://feeds.acast.com/public/shows/2?key1=value1&key2=value2",
            ),
            urls,
        )
    }

    @Test
    fun `simple entries`() {
        val input = """
            |<outline xmlUrl="https://feeds.simplecast.com/1" />
            |<outline xmlUrl="https://feeds.megaphone.fm/1" text="Title 1" />
            |<outline xmlUrl="https://feeds.acast.com/public/shows/1" type="rss" />
            |<outline xmlUrl="https://feeds.acast.com/public/shows/2?key1=value1&key2=value2" text="Title 2" type="rss" />
        """.trimMargin().asSource()

        val urls = reader.readUrls(input)

        assertEquals(
            listOf(
                "https://feeds.simplecast.com/1",
                "https://feeds.megaphone.fm/1",
                "https://feeds.acast.com/public/shows/1",
                "https://feeds.acast.com/public/shows/2?key1=value1&key2=value2",
            ),
            urls,
        )
    }

    @Test
    fun `entry split over multiple lines`() {
        val input = """
            |<outline 
            |  text="Title"
            |  xmlUrl="https://feeds.simplecast.com/1"
            |  type="rss" />
        """.trimMargin().asSource()

        val urls = reader.readUrls(input)

        assertEquals(listOf("https://feeds.simplecast.com/1"), urls)
    }

    @Test
    fun `multiple entries in the same line`() {
        val input = """
            |<outline xmlUrl="https://feeds.simplecast.com/1" />
            |<outline xmlUrl="https://feeds.megaphone.fm/1" />
            |<outline xmlUrl="https://feeds.acast.com/public/shows/1" />
            |<outline xmlUrl="https://feeds.acast.com/public/shows/2" />
        """.trimMargin().replace("\n", "").asSource()

        val urls = reader.readUrls(input)

        assertEquals(
            listOf(
                "https://feeds.simplecast.com/1",
                "https://feeds.megaphone.fm/1",
                "https://feeds.acast.com/public/shows/1",
                "https://feeds.acast.com/public/shows/2",
            ),
            urls,
        )
    }

    @Test
    fun `duplicate entries`() {
        val input = """
            |<outline xmlUrl="https://feeds.simplecast.com/1" />
            |<outline xmlUrl="https://feeds.simplecast.com/1" />
            |<outline xmlUrl="https://feeds.megaphone.fm/1" />
        """.trimMargin().asSource()

        val urls = reader.readUrls(input)

        assertEquals(
            listOf(
                "https://feeds.simplecast.com/1",
                "https://feeds.megaphone.fm/1",
            ),
            urls,
        )
    }

    @Test
    fun `single quote surrounding token`() {
        val input = "<outline xmlUrl='https://feeds.simplecast.com/1' />".asSource()

        val urls = reader.readUrls(input)

        assertEquals(listOf("https://feeds.simplecast.com/1"), urls)
    }

    @Test
    fun `double quote surrounding token`() {
        val input = "<outline xmlUrl=\"https://feeds.simplecast.com/1\" />".asSource()

        val urls = reader.readUrls(input)

        assertEquals(listOf("https://feeds.simplecast.com/1"), urls)
    }

    @Test
    fun `no starting surrounding token`() {
        val input = "<outline xmlUrl=https://feeds.simplecast.com/1' />".asSource()

        val urls = reader.readUrls(input)

        assertEquals(emptyList<String>(), urls)
    }

    @Test
    fun `mixed quote surrounding token`() {
        val input = "<outline xmlUrl='https://feeds.simplecast.com/1\" />".asSource()

        val urls = reader.readUrls(input)

        assertEquals(emptyList<String>(), urls)
    }

    @Test
    fun `no xmlUrl attribute`() {
        val input = "<outline url='https://feeds.acast.com/public/shows/1' />".asSource()

        val urls = reader.readUrls(input)

        assertEquals(emptyList<String>(), urls)
    }

    @Test
    fun `empty content`() {
        val input = "".asSource()

        val urls = reader.readUrls(input)

        assertEquals(emptyList<String>(), urls)
    }

    @Test
    fun `invalid url`() {
        val input = "<outline xmlUrl='https://feeds.simple cast.com/1' />".asSource()

        val urls = reader.readUrls(input)

        assertEquals(emptyList<String>(), urls)
    }

    @Test
    fun `mixed valid and invalid entries`() {
        val input = """
            |<outline xmlUrl="https://feeds.simplecast.com/1" />
            |<outline xmlUrl="not an url"
            |<outline />
            |<outline xmlUrl="https://feeds.megaphone.fm/1" />
        """.trimMargin().asSource()

        val urls = reader.readUrls(input)

        assertEquals(
            listOf(
                "https://feeds.simplecast.com/1",
                "https://feeds.megaphone.fm/1",
            ),
            urls,
        )
    }

    @Test
    fun `unescape lt character`() {
        val input = "<outline xmlUrl='https://feeds.simplecast.com?key=&lt;' />".asSource()

        val urls = reader.readUrls(input)

        assertEquals(listOf("https://feeds.simplecast.com?key=<"), urls)
    }

    @Test
    fun `unescape gt character`() {
        val input = "<outline xmlUrl='https://feeds.simplecast.com?key=&gt;' />".asSource()

        val urls = reader.readUrls(input)

        assertEquals(listOf("https://feeds.simplecast.com?key=>"), urls)
    }

    @Test
    fun `unescape amp character`() {
        val input = "<outline xmlUrl='https://feeds.simplecast.com?key=&amp;' />".asSource()

        val urls = reader.readUrls(input)

        assertEquals(listOf("https://feeds.simplecast.com?key=&"), urls)
    }

    @Test
    fun `unescape apos character`() {
        val input = "<outline xmlUrl='https://feeds.simplecast.com?key=&apos;' />".asSource()

        val urls = reader.readUrls(input)

        assertEquals(listOf("https://feeds.simplecast.com?key='"), urls)
    }

    @Test
    fun `unescape quot character`() {
        val input = "<outline xmlUrl='https://feeds.simplecast.com?key=&quot;' />".asSource()

        val urls = reader.readUrls(input)

        assertEquals(listOf("https://feeds.simplecast.com?key=\""), urls)
    }

    private fun String.asSource(): Source = Buffer().writeUtf8(this)
}
