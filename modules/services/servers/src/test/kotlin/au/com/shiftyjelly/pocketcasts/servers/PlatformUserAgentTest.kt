package au.com.shiftyjelly.pocketcasts.servers

import okhttp3.Headers.Companion.headersOf
import org.junit.Assert.assertEquals
import org.junit.Test

class PlatformUserAgentTest {
    @Test
    fun `include the VM version and Android release`() {
        val userAgent = PlatformUserAgent.create(vmVersion = "2.1.0", androidRelease = "16")

        assertEquals("Dalvik/2.1.0 (Linux; U; Android 16)", userAgent)
    }

    @Test
    fun `use the default VM version when it is missing`() {
        assertEquals("Dalvik/2.1.0 (Linux; U; Android 16)", PlatformUserAgent.create(vmVersion = null, androidRelease = "16"))
        assertEquals("Dalvik/2.1.0 (Linux; U; Android 16)", PlatformUserAgent.create(vmVersion = " ", androidRelease = "16"))
    }

    @Test
    fun `leave out the Android release when it is missing`() {
        assertEquals(PlatformUserAgent.FALLBACK, PlatformUserAgent.create(vmVersion = null, androidRelease = null))
        assertEquals(PlatformUserAgent.FALLBACK, PlatformUserAgent.create(vmVersion = "", androidRelease = ""))
    }

    @Test
    fun `strip characters that are not allowed in a header`() {
        val userAgent = PlatformUserAgent.create(vmVersion = "2.1.0\r\n", androidRelease = "16 Bêta ")

        assertEquals("Dalvik/2.1.0 (Linux; U; Android 16Bta)", userAgent)
        headersOf("User-Agent", userAgent)
    }

    @Test
    fun `strip characters that would break the comment`() {
        val userAgent = PlatformUserAgent.create(vmVersion = "2.1.0", androidRelease = "16); Pixel (x")

        assertEquals("Dalvik/2.1.0 (Linux; U; Android 16Pixelx)", userAgent)
    }

    @Test
    fun `fallback is a valid header value`() {
        headersOf("User-Agent", PlatformUserAgent.FALLBACK)
    }
}
