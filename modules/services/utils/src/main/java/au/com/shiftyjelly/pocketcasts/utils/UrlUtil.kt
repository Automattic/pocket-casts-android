package au.com.shiftyjelly.pocketcasts.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.inject.Inject
import kotlin.jvm.Throws
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UrlUtil @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Throws(IOException::class)
    suspend fun contentBytes(url: String) = withContext(Dispatchers.IO) {
        if (Network.isConnected(context).not()) throw NoNetworkException()
        var inputStream: InputStream? = null
        val bytes: ByteArray?
        try {
            inputStream = URL(url).openStream()
            bytes = inputStream.readBytes()
        } finally {
            inputStream?.close()
        }
        bytes
    }

    @Throws(IOException::class)
    suspend fun contentString(url: String) = withContext(Dispatchers.IO) {
        if (Network.isConnected(context).not()) throw NoNetworkException()
        buildString {
            append(URL(url).readText())
            append(System.lineSeparator())
        }
    }

    class NoNetworkException : IOException("No network connection")
}
