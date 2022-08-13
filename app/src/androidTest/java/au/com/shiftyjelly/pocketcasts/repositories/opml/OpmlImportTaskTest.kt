package au.com.shiftyjelly.pocketcasts.repositories.opml

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class OpmlImportTaskTest {

    @Test
    fun invalidXml() {
        runBlocking {
            val opml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <opml version='1.0'>
                  <head></head>
                  <body>
                    <outline
                      text="Left, Right &amp; Center"
                      type="rss"
                      xmlUrl="https://leftrightandcenter-feed.kcrw.com"
                      htmlUrl="https://www.kcrw.com/news/shows/left-right-center?utm_source=KCRW&utm_medium=RSS&utm_campaign=kcrw-show-rss" />
                    <outline 
                      type="rss" 
                      text="Tiedetrippi" 
                      xmlUrl="https://feeds.yle.fi/areena/v1/series/1-50438875.rss?lang=fi&amp;downloadable=true" />
                  </body>
                </opml>
            """.trimIndent()

            val urls = OpmlImportTask.readOpmlUrlsRegex(opml.byteInputStream())

            assertEquals(2, urls.size)
            assertEquals("https://leftrightandcenter-feed.kcrw.com", urls[0])
            assertEquals("https://feeds.yle.fi/areena/v1/series/1-50438875.rss?lang=fi&downloadable=true", urls[1])
        }
    }

    @Test
    fun invalidXmlSingleLine() {
        runBlocking {
            val opml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <opml version='1.0'><head></head><body><outline text="Left, Right &amp; Center" type="rss" xmlUrl="https://leftrightandcenter-feed.kcrw.com" htmlUrl="https://www.kcrw.com/news/shows/left-right-center?utm_source=KCRW&utm_medium=RSS&utm_campaign=kcrw-show-rss" /><outline type="rss" text="Tiedetrippi" xmlUrl="https://feeds.yle.fi/areena/v1/series/1-50438875.rss?lang=fi&amp;downloadable=true" /></body></opml>
            """.trimIndent()

            val urls = OpmlImportTask.readOpmlUrlsRegex(opml.byteInputStream())

            assertEquals(2, urls.size)
            assertEquals("https://leftrightandcenter-feed.kcrw.com", urls[0])
            assertEquals("https://feeds.yle.fi/areena/v1/series/1-50438875.rss?lang=fi&downloadable=true", urls[1])
        }
    }

    @Test
    fun validXml() {
        runBlocking {
            val opml = """
                <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
                <opml version="1.0">
                  <head>
                    <title>Pocket Casts Feeds</title>
                  </head>
                  <body>
                    <outline text="feeds">
                      <outline type="rss" text="99% Invisible" xmlUrl="https://feeds.simplecast.com/BqbsxVfO" />
                      <outline type="rss" text="Reply All" xmlUrl="https://feeds.megaphone.fm/replyall" />
                      <outline type="rss" text="Planet Money" xmlUrl="https://feeds.npr.org/510289/podcast.xml" />
                      <outline type="rss" text="This American Life" xmlUrl="http://feed.thisamericanlife.org/talpodcast" />
                      <outline type="rss" text="Freakonomics Radio" xmlUrl="https://www.omnycontent.com/d/playlist/aaea4e69-af51-495e-afc9-a9760146922b/14a43378-edb2-49be-8511-ab0d000a7030/d1b9612f-bb1b-4b85-9c0c-ab0d004ab37a/podcast.rss" />
                      <outline type="rss" text="Radiolab" xmlUrl="http://feeds.wnyc.org/radiolab" />
                      <outline type="rss" text="Serial" xmlUrl="https://feeds.simplecast.com/xl36XBC2" />
                    </outline>
                  </body>
                </opml>
            """.trimIndent()

            val urls = OpmlImportTask.readOpmlUrlsSax(opml.byteInputStream())

            assertEquals(7, urls.size)
            assertEquals("https://feeds.simplecast.com/BqbsxVfO", urls[0])
            assertEquals("https://feeds.megaphone.fm/replyall", urls[1])
            assertEquals("https://feeds.npr.org/510289/podcast.xml", urls[2])
            assertEquals("http://feed.thisamericanlife.org/talpodcast", urls[3])
            assertEquals("https://www.omnycontent.com/d/playlist/aaea4e69-af51-495e-afc9-a9760146922b/14a43378-edb2-49be-8511-ab0d000a7030/d1b9612f-bb1b-4b85-9c0c-ab0d004ab37a/podcast.rss", urls[4])
            assertEquals("http://feeds.wnyc.org/radiolab", urls[5])
            assertEquals("https://feeds.simplecast.com/xl36XBC2", urls[6])
        }
    }
}
