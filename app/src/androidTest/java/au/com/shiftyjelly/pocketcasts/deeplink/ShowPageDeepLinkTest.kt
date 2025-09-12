package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Intent.ACTION_VIEW
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShowPageDeepLinkTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun createShowPodcastsIntent() {
        val intent = ShowPodcastsDeepLink.toIntent(context)

        assertEquals(ACTION_VIEW, intent.action)
        assertEquals("podcasts", intent.getStringExtra("launch-page"))
    }

    @Test
    fun createShowDiscoverIntent() {
        val intent = ShowDiscoverDeepLink.toIntent(context)

        assertEquals(ACTION_VIEW, intent.action)
        assertEquals("search", intent.getStringExtra("launch-page"))
    }

    @Test
    fun createShowUpNextIntent() {
        val intent = ShowUpNextModalDeepLink.toIntent(context)

        assertEquals(ACTION_VIEW, intent.action)
        assertEquals("upnext", intent.getStringExtra("launch-page"))
    }

    @Test
    fun createShowFilterIntent() {
        val intent = ShowPlaylistDeepLink(playlistUuid = "id", playlistType = "smart").toIntent(context)

        assertEquals(ACTION_VIEW, intent.action)
        assertEquals("playlist", intent.getStringExtra("launch-page"))
        assertEquals("id", intent.getStringExtra("playlist_uuid"))
        assertEquals("smart", intent.getStringExtra("playlist_type"))
    }
}
