package au.com.shiftyjelly.pocketcasts.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShowPodcastDeepLinkTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun createShowPodcastIntent() {
        val intent = ShowPodcastDeepLink("Podcast ID", "Source View").toIntent(context)

        assertEquals("INTENT_OPEN_APP_PODCAST_UUID", intent.action)
        assertEquals("Podcast ID", intent.getStringExtra("podcast_uuid"))
        assertEquals("Source View", intent.getStringExtra("source_view"))
    }
}
