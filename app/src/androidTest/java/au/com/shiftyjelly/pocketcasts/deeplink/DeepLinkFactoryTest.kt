package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeepLinkFactoryTest {
    private val factory = DeepLinkFactory()

    @Test
    fun downloads() {
        val intent = Intent().setAction("INTENT_OPEN_APP_DOWNLOADING")

        val deepLink = factory.create(intent)

        assertEquals(DownloadsDeepLink, deepLink)
    }

    @Test
    fun addBookmark() {
        val intent = Intent().setAction("INTENT_OPEN_APP_ADD_BOOKMARK")

        val deepLink = factory.create(intent)

        assertEquals(AddBookmarkDeepLink, deepLink)
    }

    @Test
    fun changeBookmarkTitle() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_CHANGE_BOOKMARK_TITLE")
            .putExtra("bookmark_uuid", "bookmark-id")

        val deepLink = factory.create(intent)

        assertEquals(ChangeBookmarkTitleDeepLink("bookmark-id"), deepLink)
    }

    @Test
    fun changeBookmarkTitleWithoutBookmarkUuid() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_CHANGE_BOOKMARK_TITLE")

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }

    @Test
    fun showBookmark() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_VIEW_BOOKMARKS")
            .putExtra("bookmark_uuid", "bookmark-id")

        val deepLink = factory.create(intent)

        assertEquals(ShowBookmarkDeepLink("bookmark-id"), deepLink)
    }

    @Test
    fun showBookmarkWithoutBookmarkUuid() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_VIEW_BOOKMARKS")

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }

    @Test
    fun deleteBookmark() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_DELETE_BOOKMARK")
            .putExtra("bookmark_uuid", "bookmark-id")

        val deepLink = factory.create(intent)

        assertEquals(DeleteBookmarkDeepLink("bookmark-id"), deepLink)
    }

    @Test
    fun deleteBookmarkWithoutBookmarkUuid() {
        val intent = Intent()
            .setAction("INTENT_OPEN_APP_DELETE_BOOKMARK")

        val deepLink = factory.create(intent)

        assertNull(deepLink)
    }
}
