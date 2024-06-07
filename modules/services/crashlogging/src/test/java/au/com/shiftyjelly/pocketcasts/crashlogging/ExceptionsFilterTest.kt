package au.com.shiftyjelly.pocketcasts.crashlogging

import java.net.SocketTimeoutException
import javax.net.ssl.SSLException
import org.junit.Test

internal class ExceptionsFilterTest {
    @Test
    fun shouldIgnoreExceptions() {
        // ignore java.io.IOException exceptions
        val ioException =
            Exception(RuntimeException(SocketTimeoutException("failed to connect to api.pocketcasts.com")))
        assert(ExceptionsFilter.shouldIgnoreExceptions(ioException))

        // don't ignore other exceptions
        val nullException = Exception(RuntimeException(NullPointerException()))
        assert(!ExceptionsFilter.shouldIgnoreExceptions(nullException))
    }

    @Test
    fun shouldIgnoreSSLException() {
        assert(
            ExceptionsFilter.shouldIgnoreExceptions(
                SSLException("failed to connect to api.pocketcasts.com"),
            ),
        )
    }
}
