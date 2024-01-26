package au.com.shiftyjelly.pocketcasts.utils

import java.net.SocketTimeoutException
import org.junit.Test

class SentryHelperTest {

    @Test
    fun shouldIgnoreExceptions() {
        // ignore java.io.IOException exceptions
        val ioException = Exception(RuntimeException(SocketTimeoutException("failed to connect to api.pocketcasts.com")))
        assert(SentryHelper.shouldIgnoreExceptions(ioException))

        // don't ignore other exceptions
        val nullException = Exception(RuntimeException(NullPointerException()))
        assert(!SentryHelper.shouldIgnoreExceptions(nullException))
    }
}
