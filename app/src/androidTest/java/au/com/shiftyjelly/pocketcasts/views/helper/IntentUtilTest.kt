package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test

class IntentUtilTest {

    @Test
    fun openWebPage() {
        val intent = IntentUtil.openWebPage("http://www.google.com")
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("http", intent.data?.scheme)
        assertEquals("www.google.com", intent.data?.host)
    }

    @Test
    fun openWebPageWithoutScheme() {
        val intent = IntentUtil.openWebPage("www.google.com")
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("http", intent.data?.scheme)
        assertEquals("www.google.com", intent.data?.host)
    }

    @Test
    fun testSubscribeUrlWithParams() {
        val intent = Intent(Intent.ACTION_VIEW)
        val url = "pktc://subscribe/mypodcast.memberfulcontent.com/rss/6618?someKey=someValue"
        intent.data = Uri.parse(url)

        val parsed = IntentUtil.getPodloveUrl(intent)
        assertEquals("http://mypodcast.memberfulcontent.com/rss/6618?someKey=someValue", parsed)
    }

    @Test
    fun testSubscribeUrlWithoutParams() {
        val intent = Intent(Intent.ACTION_VIEW)
        val url = "pktc://subscribe/mypodcast.com/rss/123"
        intent.data = Uri.parse(url)

        val parsed = IntentUtil.getPodloveUrl(intent)
        assertEquals("http://mypodcast.com/rss/123", parsed)
    }

    @Test
    fun testSubscribeUrlWithParamsHttps() {
        val intent = Intent(Intent.ACTION_VIEW)
        val url = "pktc://subscribehttps/mypodcast.memberfulcontent.com/rss/6618?someKey=someValue"
        intent.data = Uri.parse(url)

        val parsed = IntentUtil.getPodloveUrl(intent)
        assertEquals("https://mypodcast.memberfulcontent.com/rss/6618?someKey=someValue", parsed)
    }

    @Test
    fun testSubscribeUrlWithoutParamsHttps() {
        val intent = Intent(Intent.ACTION_VIEW)
        val url = "pktc://subscribehttps/mypodcast.com/rss/123"
        intent.data = Uri.parse(url)

        val parsed = IntentUtil.getPodloveUrl(intent)
        assertEquals("https://mypodcast.com/rss/123", parsed)
    }
}
