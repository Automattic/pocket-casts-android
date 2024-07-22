package au.com.shiftyjelly.pocketcasts.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadsDeepLinkTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun createIntent() {
        val intent = DownloadsDeepLink.toIntent(context)

        assertEquals("INTENT_OPEN_APP_DOWNLOADING", intent.action)
    }
}
