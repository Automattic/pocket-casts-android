package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShowEpisodeDeepLinkTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun createShowEpisodeIntent() {
        val intent = ShowEpisodeDeepLink("Episode ID", "Podcast ID", "Source View", autoPlay = true).toIntent(context)

        assertEquals("INTENT_OPEN_APP_EPISODE_UUID", intent.action)
        assertEquals(Intent.FLAG_ACTIVITY_SINGLE_TOP, intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP)
        assertEquals("Episode ID", intent.getStringExtra("episode_uuid"))
        assertEquals("Podcast ID", intent.getStringExtra("podcast_uuid"))
        assertEquals("Source View", intent.getStringExtra("source_view"))
        assertEquals(true, intent.getBooleanExtra("auto_play", false))
    }

    @Test
    fun createShowEpisodeUri() {
        val uri = ShowEpisodeDeepLink("episode-id", "podcast-id", "source-view", autoPlay = true, startTimestamp = 1.seconds, endTimestamp = 20.seconds).toUri("pca.st")

        assertEquals(Uri.parse("https://pca.st/episode/episode-id?t=1%2C20&auto_play=true&source_view=source-view"), uri)
    }

    @Test
    fun createShowEpisodeUriWithoutTimestamps() {
        val uri = ShowEpisodeDeepLink("episode-id", "podcast-id", "source-view", autoPlay = false).toUri("pca.st")

        assertEquals(Uri.parse("https://pca.st/episode/episode-id?auto_play=false&source_view=source-view"), uri)
    }
}
