package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Intent.ACTION_VIEW
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloudFilesDeepLinkTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun createIntent() {
        val intent = CloudFilesDeepLink.toIntent(context)

        assertEquals(ACTION_VIEW, intent.action)
        assertEquals(Uri.parse("pktc://cloudfiles"), intent.data)
    }
}
