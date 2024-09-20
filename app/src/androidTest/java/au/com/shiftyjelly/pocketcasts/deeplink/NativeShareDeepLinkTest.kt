package au.com.shiftyjelly.pocketcasts.deeplink

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeShareDeepLinkTest {
    @Test
    fun sharePathWithSinglePathSegment() {
        val deepLink = NativeShareDeepLink(Uri.parse("https://hello.com/segment"))

        assertEquals("/social/share/show/segment", deepLink.sharePath)
    }

    @Test
    fun sharePathWithMultiplePathSegment() {
        val deepLink = NativeShareDeepLink(Uri.parse("https://hello.com/segment1/segment2"))

        assertEquals("/segment1/segment2", deepLink.sharePath)
    }
}
