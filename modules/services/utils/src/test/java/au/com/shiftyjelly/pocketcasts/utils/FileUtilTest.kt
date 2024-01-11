package au.com.shiftyjelly.pocketcasts.utils

import java.io.File
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
}
