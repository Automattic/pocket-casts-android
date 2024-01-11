package au.com.shiftyjelly.pocketcasts.utils

import java.io.File
import kotlin.random.Random
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.buffer
import okio.sink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileUtilTest {
    @get:Rule val tempDir = TemporaryFolder()

    @Test
    fun `delete existing file`() {
        val file = tempDir.newFile()

        FileUtil.deleteFileByPath(file.path)

        assertFalse("File $file exists", file.exists())
    }

    @Test
    fun `delete non existing file`() {
        val file = tempDir.newFile()

        FileUtil.deleteFileByPath(file.path + "test")

        assertTrue("File $file does not exist", file.exists())
    }

    @Test
    fun `delete directory contents`() {
        val dir = tempDir.newFolder()
        val contents = listOf(
            File(dir, "test1").also(File::createNewFile),
            File(dir, "test2").also(File::createNewFile),
            File(dir, "test3").also(File::createNewFile),
        )

        FileUtil.deleteDirectoryContents(dir.path)

        assertFalse("At least one file still exists", contents.any(File::exists))
    }

    @Test
    fun `keep nomedia file when deleting directory contents`() {
        listOf(".nomedia", ".NOMEDIA", ".nOmEdIa").forEach { name ->
            val dir = tempDir.newFolder()
            val file = File(dir, name).also(File::createNewFile)

            FileUtil.deleteDirectoryContents(dir.path)

            assertTrue("nomedia file $file does not exist", file.exists())
        }
    }

    @Test
    fun `delete files with nomedia extension when deleting directory contests`() {
        val dir = tempDir.newFolder()
        val contents = listOf(
            File(dir, "test1.nomedia").also(File::createNewFile),
            File(dir, "test2.nomedia").also(File::createNewFile),
            File(dir, "test3.nomedia").also(File::createNewFile),
        )

        FileUtil.deleteDirectoryContents(dir.path)

        assertFalse("At least one file still exists", contents.any(File::exists))
    }

    @Test
    fun `read file to output stream`() {
        val file = tempDir.newFile().also { it.writeRandomBytes(1024) }
        val outputBuffer = Buffer()

        FileUtil.readFileTo(file, outputBuffer.outputStream())

        assertEquals("File and output stream have different contents", file.snapshot(), outputBuffer.snapshot())
    }

    private fun File.writeRandomBytes(count: Int) {
        sink().buffer().use { it.write(Random.nextBytes(count)) }
    }

    private fun File.snapshot() = readBytes().toByteString()
}
