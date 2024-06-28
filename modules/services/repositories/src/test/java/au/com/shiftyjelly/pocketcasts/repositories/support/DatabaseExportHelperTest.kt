package au.com.shiftyjelly.pocketcasts.repositories.support

import android.content.Context
import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.utils.FileUtilWrapper
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private const val TEST_OUTPUT_FOLDER = "outputFolder"
private const val DATABASE_PATH = "/path/to/database"

@RunWith(MockitoJUnitRunner::class)
class DatabaseExportHelperTest {

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var fileUtil: FileUtilWrapper

    @Mock
    private lateinit var appDatabase: AppDatabase

    @Mock
    private lateinit var sharedPrefs: SharedPreferences

    private lateinit var exportFolder: File

    private val mockFile: File = mock {
        on { exists() } doReturn true
        on { path } doReturn ""
    }

    private lateinit var databaseExportHelper: DatabaseExportHelper

    @Before
    fun setUp() {
        exportFolder = temporaryFolder.newFolder(TEST_OUTPUT_FOLDER)

        doNothing().whenever(fileUtil).copy(any(), any())
        doNothing().whenever(fileUtil).copyPreferences(any(), any())
        doNothing().whenever(fileUtil).deleteFileByPath(any())
        doNothing().whenever(fileUtil).deleteDirectoryContents(any())
    }

    @Test
    fun `prepareExportFolder copies logs, shared prefs, database files into export folder`() =
        runTest {
            whenever(mockFile.name).thenReturn(DATABASE_PATH)
            initDatabaseExportHelper(mockFile)

            databaseExportHelper.prepareExportFolder(exportFolder)

            verify(fileUtil).copy(
                eq(File(context.filesDir, "logs/debug.log")),
                eq(File(exportFolder, "/logs.txt")),
            )
            verify(fileUtil).copyPreferences(
                eq(sharedPrefs),
                eq(File(exportFolder, "/preferences.xml")),
            )
            verify(fileUtil).copy(eq(mockFile), eq(File(exportFolder, DATABASE_PATH)))
        }

    @Test
    fun `getExportFile zips export folder`() = runTest {
        initDatabaseExportHelper()

        databaseExportHelper.getExportFile(exportFolder)

        verify(fileUtil).zip(eq(exportFolder), any())
    }

    @Test
    fun `export folder is deleted after it is zipped`() = runTest {
        initDatabaseExportHelper()

        databaseExportHelper.zip(exportFolder, mockFile)

        verify(fileUtil).deleteDirectoryContents(exportFolder.path)
    }

    @Test
    fun `file sent to cleanup is deleted`() = runTest {
        initDatabaseExportHelper()

        databaseExportHelper.cleanup(mockFile)

        verify(fileUtil).deleteFileByPath(mockFile.path)
    }

    private fun initDatabaseExportHelper(
        dbFile: File? = null,
    ) {
        dbFile?.let { whenever(appDatabase.databaseFiles()).thenReturn(listOf(dbFile)) }
        databaseExportHelper = DatabaseExportHelper(context, sharedPrefs, fileUtil, appDatabase)
    }

    @After
    fun tearDown() {
        temporaryFolder.delete()
    }
}
