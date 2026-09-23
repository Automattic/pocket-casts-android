package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WhatsNewActionEventTest {
    @Test
    fun `every supported catalog event maps to its action`() {
        val keys = listOf("open_podcasts", "open_discover", "open_up_next", "open_playlists", "open_profile", "open_settings")

        assertEquals(WhatsNewActionEvent.entries, keys.map(WhatsNewActionEvent::fromKey))
    }

    @Test
    fun `the upgrade action is not supported yet`() {
        assertNull(WhatsNewActionEvent.fromKey("open_upsell"))
    }

    @Test
    fun `an unknown event is not supported`() {
        assertNull(WhatsNewActionEvent.fromKey("open_time_machine"))
    }
}
