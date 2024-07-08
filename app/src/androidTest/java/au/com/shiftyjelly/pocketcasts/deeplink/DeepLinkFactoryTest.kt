package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
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
}
