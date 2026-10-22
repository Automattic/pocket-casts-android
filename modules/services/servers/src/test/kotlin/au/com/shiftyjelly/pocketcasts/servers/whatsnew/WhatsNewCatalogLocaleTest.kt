package au.com.shiftyjelly.pocketcasts.servers.whatsnew

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsNewCatalogLocaleTest {
    @Test
    fun `a language the feed publishes is asked for by name`() {
        assertEquals("fr", catalogName(Locale.FRENCH))
        assertEquals("de", catalogName(Locale.GERMAN))
    }

    @Test
    fun `a region the feed does not publish separately reads its language`() {
        assertEquals("en", catalogName(Locale.UK))
        assertEquals("es", catalogName(Locale.forLanguageTag("es-MX")))
        assertEquals("fr", catalogName(Locale.CANADA_FRENCH))
        assertEquals("pt", catalogName(Locale.forLanguageTag("pt-PT")))
    }

    @Test
    fun `brazilian portuguese has a catalog of its own`() {
        assertEquals("pt-br", catalogName(Locale.forLanguageTag("pt-BR")))
    }

    @Test
    fun `chinese is asked for by the region its catalog is published under`() {
        assertEquals("zh-cn", catalogName(Locale.SIMPLIFIED_CHINESE))
        assertEquals("zh-cn", catalogName(Locale.forLanguageTag("zh-Hans-CN")))
        assertEquals("zh-tw", catalogName(Locale.TRADITIONAL_CHINESE))
        assertEquals("zh-tw", catalogName(Locale.forLanguageTag("zh-Hant-HK")))
        assertEquals("zh-tw", catalogName(Locale.forLanguageTag("zh-HK")))
    }

    @Test
    fun `simplified chinese keeps its catalog in a traditional chinese region`() {
        assertEquals("zh-cn", catalogName(Locale.forLanguageTag("zh-Hans-HK")))
        assertEquals("zh-cn", catalogName(Locale.forLanguageTag("zh-Hans-MO")))
    }

    @Test
    fun `a language the feed does not publish yet is still asked for`() {
        assertEquals("cy", catalogName(Locale.forLanguageTag("cy")))
    }

    @Test
    fun `a locale with no language of its own reads the english catalog`() {
        assertEquals("en", catalogName(Locale.ROOT))
    }

    private fun catalogName(locale: Locale) = WhatsNewCatalogLocale.catalogName(locale)
}
