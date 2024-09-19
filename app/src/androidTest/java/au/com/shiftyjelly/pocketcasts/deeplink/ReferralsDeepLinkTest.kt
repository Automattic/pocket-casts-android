package au.com.shiftyjelly.pocketcasts.deeplink

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReferralsDeepLinkTest {
    @Test
    fun createSignInUri() {
        val uri = ReferralsDeepLink("abc").toUri("pocketcasts.com")

        assertEquals(Uri.parse("https://pocketcasts.com/redeem/abc"), uri)
    }
}
