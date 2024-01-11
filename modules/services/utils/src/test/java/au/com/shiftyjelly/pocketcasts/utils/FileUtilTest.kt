package au.com.shiftyjelly.pocketcasts.utils

import java.io.File
import kotlin.random.Random
import okio.Buffer
import okio.ByteString.Companion.toByteString
import okio.FileNotFoundException
import okio.IOException
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
    fun `file is copied`() {
        val file1 = tempDir.newFile().also { it.writeRandomBytes(100) }
        val file2 = tempDir.newFile()
        val originalSnapshot = file1.snapshot()

        FileUtil.copy(file1, file2)

        assertEquals("Original file has different content", originalSnapshot, file1.snapshot())
        assertEquals("Files contents differ", file1.snapshot(), file2.snapshot())
    }

    @Test
    fun `directory is copied`() {
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
    fun `fail copying file to a directory`() {
        val file = tempDir.newFile()
        val dir = tempDir.newFolder()

        val exception = assertThrows(IOException::class.java) {
            FileUtil.copy(file, dir)
        }

        assertEquals("Can't copy from file $file to file $dir", exception.message)
    }

    @Test
    fun `fail copying directory to a file`() {
        val file = tempDir.newFile()
        val dir = tempDir.newFolder()

        val exception = assertThrows(IOException::class.java) {
            FileUtil.copy(dir, file)
        }

        assertEquals("Can't copy from dir $dir to file $file", exception.message)
    }

    @Test
    fun `get file name without extension for a file with a single dot`() {
        val file = tempDir.newFile("hello.there")

        val fileName = FileUtil.getFileNameWithoutExtension(file)

        assertEquals("hello", fileName)
    }

    @Test
    fun `get file name without extension for a file with multiple dots`() {
        val file = tempDir.newFile("hello.there..traveller")

        val fileName = FileUtil.getFileNameWithoutExtension(file)

        assertEquals("hello.there.", fileName)
    }

    @Test
    fun `get file name without extension for a file with no dots`() {
        val file = tempDir.newFile("hello")

        val fileName = FileUtil.getFileNameWithoutExtension(file)

        assertEquals("hello", fileName)
    }

    @Test
    fun `get file name without extension for a directory with a single dot`() {
        val dir = tempDir.newFolder("hello.there")

        val dirName = FileUtil.getFileNameWithoutExtension(dir)

        assertEquals("hello", dirName)
    }

    @Test
    fun `get directory name without extension for a directory with multiple dots`() {
        val dir = tempDir.newFolder("hello.there..traveller")

        val dirName = FileUtil.getFileNameWithoutExtension(dir)

        assertEquals("hello.there.", dirName)
    }

    @Test
    fun `get directory name without extension for a directory with no dots`() {
        val dir = tempDir.newFolder("hello")

        val dirName = FileUtil.getFileNameWithoutExtension(dir)

        assertEquals("hello", dirName)
    }

    @Test
    fun `compute empty directory size`() {
        val dir = tempDir.newFolder()

        val size = FileUtil.folderSize(dir)

        assertEquals(0, size)
    }

    @Test
    fun `compute directory size with an empty file`() {
        val dir = tempDir.newFolder().apply {
            File(this, "file").also(File::createNewFile)
        }

        val size = FileUtil.folderSize(dir)

        assertEquals(0, size)
    }

    @Test
    fun `compute directory size with a non-empty file`() {
        val dir = tempDir.newFolder().apply {
            File(this, "file").writeRandomBytes(100)
        }

        val size = FileUtil.folderSize(dir)

        assertEquals(100, size)
    }

    @Test
    fun `compute directory size with complex structure`() {
        val dir = tempDir.newFolder().apply {
            File(this, "file1").writeRandomBytes(1)
            File(this, "file2").writeRandomBytes(10)

            File(this, "inner1").also(File::mkdirs).apply {
                File(this, "file3").writeRandomBytes(100)

                File(this, "inner2").also(File::mkdirs).apply {
                    File(this, "file4").writeRandomBytes(1000)
                    File(this, "file5").writeRandomBytes(10000)
                }
            }

            File(this, "inner3").also(File::mkdirs).apply {
                File(this, "file6").writeRandomBytes(100000)
                File(this, "file7").writeRandomBytes(1000000)
            }
        }

        val size = FileUtil.folderSize(dir)

        assertEquals(1111111, size)
    }

    @Test
    fun `do not compute size of a file`() {
        val file = tempDir.newFile()
        file.writeRandomBytes(1024)

        val size = FileUtil.folderSize(file)

        assertEquals(0, size)
    }

    private fun File.writeRandomBytes(count: Int) {
        sink().buffer().use { it.write(Random.nextBytes(count)) }
    }

    private fun File.snapshot() = readBytes().toByteString()
}
