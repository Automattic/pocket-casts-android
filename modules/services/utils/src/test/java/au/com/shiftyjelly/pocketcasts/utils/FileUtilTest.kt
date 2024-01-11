package au.com.shiftyjelly.pocketcasts.utils

import java.io.File
import kotlin.random.Random
import okio.Buffer
import okio.ByteString.Companion.toByteString
import okio.FileNotFoundException
import okio.buffer
import okio.sink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
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

    @Test
    fun `reading file to output stream does not delete file content`() {
        val file = tempDir.newFile().also { it.writeRandomBytes(1024) }
        val originalSnapshot = file.snapshot()

        FileUtil.readFileTo(file, Buffer().outputStream())

        assertEquals("Original file content was changed", originalSnapshot, file.snapshot())
    }

    @Test
    fun `file is copied using copy function`() {
        val file1 = tempDir.newFile().also { it.writeRandomBytes(100) }
        val file2 = tempDir.newFile()
        val originalSnapshot = file1.snapshot()

        FileUtil.copy(file1, file2)

        assertEquals("Original file has different content", originalSnapshot, file1.snapshot())
        assertEquals("Files contents differ", file1.snapshot(), file2.snapshot())
    }

    @Test
    fun `file is copied using copyFile function`() {
        val file1 = tempDir.newFile().also { it.writeRandomBytes(100) }
        val file2 = tempDir.newFile()
        val originalSnapshot = file1.snapshot()

        FileUtil.copyFile(file1, file2)

        assertEquals("Original file has different content", originalSnapshot, file1.snapshot())
        assertEquals("Files contents differ", file1.snapshot(), file2.snapshot())
    }

    @Test
    fun `directory is copied using copy function`() {
        val dir1 = tempDir.newFolder().apply {
            File(this, "file1").writeRandomBytes(100)
            File(this, "file2").writeRandomBytes(200)
            val innerDir = File(this, "inner").also(File::mkdirs)
            File(innerDir, "file3").writeRandomBytes(300)
        }
        val dir2 = tempDir.newFolder()

        FileUtil.copy(dir1, dir2)

        val comparisonPairs = listOf(
            File(dir1, "file1") to File(dir2, "file1"),
            File(dir1, "file2") to File(dir2, "file2"),
            File(File(dir1, "inner"), "file3") to File(File(dir2, "inner"), "file3"),
        )
        comparisonPairs.forEach { (srcFile, dstFile) ->
            assertFalse("Original file has no content", srcFile.snapshot().size == 0)
            assertEquals("Files contents differ", srcFile.snapshot(), dstFile.snapshot())
        }
    }

    @Test
    fun `directory is copied using copyDirectory function`() {
        val dir1 = tempDir.newFolder().apply {
            File(this, "file1").writeRandomBytes(100)
            File(this, "file2").writeRandomBytes(200)
            val innerDir = File(this, "inner").also(File::mkdirs)
            File(innerDir, "file3").writeRandomBytes(300)
        }
        val dir2 = tempDir.newFolder()

        FileUtil.copyDirectory(dir1, dir2)

        val comparisonPairs = listOf(
            File(dir1, "file1") to File(dir2, "file1"),
            File(dir1, "file2") to File(dir2, "file2"),
            File(File(dir1, "inner"), "file3") to File(File(dir2, "inner"), "file3"),
        )
        comparisonPairs.forEach { (srcFile, dstFile) ->
            assertFalse("Original file has no content", srcFile.snapshot().size == 0)
            assertEquals("Files contents differ", srcFile.snapshot(), dstFile.snapshot())
        }
    }

    @Test
    fun `fail copying file to a directory using copy function`() {
        val file = tempDir.newFile()
        val dir = tempDir.newFolder()

        val exception = assertThrows(FileNotFoundException::class.java) {
            FileUtil.copy(file, dir)
        }

        assertEquals("$dir (Is a directory)", exception.message)
    }

    @Test
    fun `fail copying file to a directory using copyFile function`() {
        val file = tempDir.newFile()
        val dir = tempDir.newFolder()

        val exception = assertThrows(FileNotFoundException::class.java) {
            FileUtil.copyFile(file, dir)
        }

        assertEquals("$dir (Is a directory)", exception.message)
    }

    @Test
    fun `fail copying file to a directory using copyDirectory function`() {
        val file = tempDir.newFile()
        val dir = tempDir.newFolder()

        val exception = assertThrows(FileNotFoundException::class.java) {
            FileUtil.copyDirectory(file, dir)
        }

        assertEquals("$dir (Is a directory)", exception.message)
    }

    @Test
    fun `fail copying directory to a file using copy function`() {
        val file = tempDir.newFile()
        val dir = tempDir.newFolder().apply {
            File(this, "file").writeRandomBytes(100)
        }

        val exception = assertThrows(FileNotFoundException::class.java) {
            FileUtil.copy(dir, file)
        }

        assertEquals("$file/file (Not a directory)", exception.message)
    }

    @Test
    fun `fail copying directory to a file using copyFile function`() {
        val file = tempDir.newFile()
        val dir = tempDir.newFolder()

        val exception = assertThrows(FileNotFoundException::class.java) {
            FileUtil.copyFile(dir, file)
        }

        assertEquals("$dir (Is a directory)", exception.message)
    }

    @Test
    fun `fail copying a directory to a file using copyDirectory function`() {
        val file = tempDir.newFile()
        val dir = tempDir.newFolder().apply {
            File(this, "file").writeRandomBytes(100)
        }

        val exception = assertThrows(FileNotFoundException::class.java) {
            FileUtil.copyDirectory(dir, file)
        }

        assertEquals("$file/file (Not a directory)", exception.message)
    }

    @Test
    fun `get file name without extension for a file with a single dot`() {
        val file = tempDir.newFile("hello.there")

        val fileName = FileUtil.getFileNameWithoutExtension(file)

        assertEquals("hello", fileName)
    }

    private fun File.writeRandomBytes(count: Int) {
        sink().buffer().use { it.write(Random.nextBytes(count)) }
    }

    private fun File.snapshot() = readBytes().toByteString()
}
