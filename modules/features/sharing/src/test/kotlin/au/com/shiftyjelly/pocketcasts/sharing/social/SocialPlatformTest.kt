package au.com.shiftyjelly.pocketcasts.sharing.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialPlatformTest {
    @Test
    fun `social platfrom are correctly prioritzed`() {
        val sortedPlatfroms = SocialPlatform.entries

        assertEquals(
            listOf(
                SocialPlatform.Instagram,
                SocialPlatform.WhatsApp,
                SocialPlatform.Telegram,
                SocialPlatform.X,
                SocialPlatform.Tumblr,
                SocialPlatform.PocketCasts,
                SocialPlatform.More,
            ),
            sortedPlatfroms,
        )
    }
}
