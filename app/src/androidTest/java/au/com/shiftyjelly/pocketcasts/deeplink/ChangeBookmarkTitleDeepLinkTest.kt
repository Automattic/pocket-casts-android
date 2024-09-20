package au.com.shiftyjelly.pocketcasts.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChangeBookmarkTitleDeepLinkTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun createChangeBookmarkTitleIntent() {
        val intent = ChangeBookmarkTitleDeepLink("bookmark-id").toIntent(context)

        assertEquals("INTENT_OPEN_APP_CHANGE_BOOKMARK_TITLE", intent.action)
        assertEquals("bookmark-id", intent.getStringExtra("bookmark_uuid"))
        assertEquals("bookmark_uuid_bookmark-id", intent.getStringExtra("NOTIFICATION_TAG"))
    }
}
