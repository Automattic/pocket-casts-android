package au.com.shiftyjelly.pocketcasts.account

import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public object TokenSerializer : Serializer<String> {
    override val defaultValue: String = ""

    override suspend fun readFrom(input: InputStream): String =
        InputStreamReader(input).readText()

    override suspend fun writeTo(t: String, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(t.toByteArray())
        }
    }
}
