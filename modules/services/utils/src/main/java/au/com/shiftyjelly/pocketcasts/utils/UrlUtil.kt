package au.com.shiftyjelly.pocketcasts.utils

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
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

    @Throws(IOException::class)
    suspend fun contentString(url: String) = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        val sb = StringBuilder()
        var line: String?

        try {
            inputStream = URL(url).openStream()
            val reader = BufferedReader(InputStreamReader(inputStream))
            while ((reader.readLine().also { line = it }) != null) {
                sb.append(line).append(System.lineSeparator())
            }
        } finally {
            inputStream?.close()
        }

        sb.toString()
    }
}
