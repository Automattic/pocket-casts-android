package au.com.shiftyjelly.pocketcasts.models.entity

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class AnonymousBumpStatTest {
    private val originalLocale = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `withBumpName lowercases with root locale`() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))

        val bumpStat = AnonymousBumpStat(name = "DISCOVER_LIST_IMPRESSION").withBumpName()

        assertEquals("pcandroid_discover_list_impression_bump", bumpStat.name)
    }

    @Test
    fun `withBumpName keeps already lowercase names unchanged`() {
        val bumpStat = AnonymousBumpStat(name = "discover_list_impression").withBumpName()

        assertEquals("pcandroid_discover_list_impression_bump", bumpStat.name)
    }
}
