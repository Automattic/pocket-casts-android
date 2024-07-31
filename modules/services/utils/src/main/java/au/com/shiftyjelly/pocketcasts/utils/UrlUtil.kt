package au.com.shiftyjelly.pocketcasts.utils

import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UrlUtil @Inject constructor() {
    suspend fun contentBytes(url: String) = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        var bytes: ByteArray? = null
        try {
            inputStream = URL(url).openStream()
            bytes = inputStream.readBytes()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
        bytes
    }

    suspend fun contentString(url: String) = withContext(Dispatchers.IO) {
        buildString {
            append(URL(url).readText())
            append(System.lineSeparator())
        }
    }
}
