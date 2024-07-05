package au.com.shiftyjelly.pocketcasts.views.helper

import android.content.Intent
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
}
