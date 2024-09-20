package au.com.shiftyjelly.pocketcasts.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteookmarkDeepLinkTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun createShowBookmarkIntent() {
        val intent = DeleteBookmarkDeepLink("bookmark-id").toIntent(context)

        assertEquals("INTENT_OPEN_APP_DELETE_BOOKMARK", intent.action)
        assertEquals("bookmark-id", intent.getStringExtra("bookmark_uuid"))
        assertEquals("bookmark_uuid_bookmark-id", intent.getStringExtra("NOTIFICATION_TAG"))
    }
}
