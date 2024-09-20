package au.com.shiftyjelly.pocketcasts.deeplink

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInDeepLinkTest {
    @Test
    fun createSignInUri() {
        val uri = SignInDeepLink("source-view").toUri("pca.st")

        assertEquals(Uri.parse("https://pca.st/sign-in?source_view=source-view"), uri)
    }
}
