package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WhatsNewActionEventTest {
    @Test
    fun `every supported catalog event maps to its action`() {
        val keys = listOf("open_podcasts", "open_discover", "open_up_next", "open_playlists", "open_profile", "open_settings", "open_upsell", "create_playlist")

        assertEquals(WhatsNewActionEvent.entries, keys.map(WhatsNewActionEvent::fromKey))
    }

    @Test
    fun `every action reports its catalog key`() {
        WhatsNewActionEvent.entries.mapNotNull { event -> event.analyticsValue?.let { event to it } }.forEach { (event, action) ->
            assertEquals(event.key, action.toString())
        }
    }

    @Test
    fun `an unknown event is not supported`() {
        assertNull(WhatsNewActionEvent.fromKey("open_time_machine"))
    }
}
