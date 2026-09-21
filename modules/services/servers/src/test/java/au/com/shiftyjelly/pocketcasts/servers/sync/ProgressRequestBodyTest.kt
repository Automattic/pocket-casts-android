package au.com.shiftyjelly.pocketcasts.servers.sync

import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProgressRequestBodyTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `reports cumulative progress as the file is written`() {
        val bytes = ByteArray(20_000) { index -> index.toByte() }
        val file = temporaryFolder.newFile().apply { writeBytes(bytes) }
        val progressValues = mutableListOf<Float>()
        val requestBody = ProgressRequestBody.create("audio/mp3".toMediaType(), file) { progress ->
            progressValues += progress
        }

        val output = Buffer()
        requestBody.writeTo(output)

        assertArrayEquals(bytes, output.readByteArray())
        assertEquals(1f, progressValues.last())
        assertEquals(progressValues.sorted(), progressValues)
    }
}
