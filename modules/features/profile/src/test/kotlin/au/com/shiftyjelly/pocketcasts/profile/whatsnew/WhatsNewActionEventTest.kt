package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WhatsNewActionEventTest {
    @Test
    fun `every supported catalog event maps to its action`() {
        val keys = listOf("open_podcasts", "open_discover", "open_up_next", "open_playlists", "open_profile", "open_settings", "open_upsell", "create_playlist", "open_link", "open_networks")

        assertEquals(WhatsNewActionEvent.entries, keys.map(WhatsNewActionEvent::fromKey))
    }

    @Test
    fun `every action reports its catalog key`() {
        WhatsNewActionEvent.entries.forEach { event ->
            assertEquals(event.key, event.analyticsValue.toString())
        }
    }

    @Test
    fun `the link action matches the catalog's link type`() {
        assertEquals(WhatsNewAction.OPEN_LINK, WhatsNewActionEvent.OpenLink.key)
    }

    @Test
    fun `an unknown event is not supported`() {
        assertNull(WhatsNewActionEvent.fromKey("open_time_machine"))
    }
}
