package au.com.shiftyjelly.pocketcasts.servers.di

import au.com.shiftyjelly.pocketcasts.servers.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class UserAgentTest {
    @Test
    fun `public user agent identifies Android without changing internal user agent`() {
        assertEquals("Pocket Casts (Android)", InterceptorModule.PC_PUBLIC_USER_AGENT)
        assertEquals(
            "Pocket Casts/Android/${BuildConfig.VERSION_NAME}",
            InterceptorModule.PC_INTERNAL_USER_AGENT,
        )
    }
}
