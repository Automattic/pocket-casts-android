package au.com.shiftyjelly.pocketcasts.utils

import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileUtilTest {
    @get:Rule val tempDir = TemporaryFolder()

    @Test
    fun `delete existing file`() {
        val file = tempDir.newFile()

        FileUtil.deleteFileByPath(file.path)

        assertFalse("File $file is not deleted", file.exists())
    }
}
