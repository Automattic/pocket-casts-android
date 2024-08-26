package au.com.shiftyjelly.pocketcasts.sharing.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialPlatformTest {
    @Test
    fun `social platform are correctly prioritzed`() {
        val sortedplatforms = SocialPlatform.entries

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
            sortedplatforms,
        )
    }
}
