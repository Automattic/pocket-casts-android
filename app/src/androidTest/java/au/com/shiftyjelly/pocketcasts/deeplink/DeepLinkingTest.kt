package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeepLinkingTest {
    private val factory = DeepLinkFactory()
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun goToDownloadedEpisodes() {
        val intent = Intent().setAction("INTENT_OPEN_APP_DOWNLOADING")

        val deepLink = factory.createDeepLink(intent)

        assertEquals(GoToDownloadedEpisodes, deepLink)
    }

    @Test
    fun createDownloadedEpisodesIntent() {
        val intent = GoToDownloadedEpisodes.toIntent(context)

        assertEquals("INTENT_OPEN_APP_DOWNLOADING", intent.action)
    }

    @Test
    fun failThisTest() {
        val intent = GoToDownloadedEpisodes.toIntent(context)

        assertEquals("???", intent.action)
    }
}
