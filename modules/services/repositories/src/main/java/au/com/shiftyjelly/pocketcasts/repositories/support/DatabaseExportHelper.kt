package au.com.shiftyjelly.pocketcasts.repositories.support

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.preferences.di.PublicSharedPreferences
import au.com.shiftyjelly.pocketcasts.utils.FileUtilWrapper
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class DatabaseExportHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    @PublicSharedPreferences private val sharedPrefs: SharedPreferences,
    private val fileUtil: FileUtilWrapper,
    private val appDatabase: AppDatabase,
) {
    companion object {
        private const val TAG = "DatabaseExport"
        const val EXPORT_FOLDER_NAME = "PocketCastsDatabaseExport"
    }

    suspend fun getExportFile(
        exportFolderFile: File = File(context.filesDir, EXPORT_FOLDER_NAME),
    ): File? = withContext(Dispatchers.IO) {

        val exportFolder = prepareExportFolder(exportFolderFile) ?: return@withContext null

        try {
            val email = File(context.filesDir, "email")
            val outputZipFile = File(email, "$EXPORT_FOLDER_NAME.zip")

            val zipFile = zip(exportFolder, outputZipFile)
            return@withContext zipFile
        } catch (e: Exception) {
            LogBuffer.e(TAG, "Could not generate zip file: $e")
            null
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun zip(
        exportFolder: File,
        outputZipFile: File,
    ): File? {
        // Cleanup previous zip file, if exists
        if (outputZipFile.exists()) cleanup(outputZipFile)

        val isZipped = fileUtil.zip(exportFolder, outputZipFile)

        // Cleanup export folder after zip
        cleanup(exportFolder)

        return if (isZipped) outputZipFile else null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun prepareExportFolder(
        exportFolder: File,
    ) = withContext(Dispatchers.IO) {
        try {
            exportFolder.mkdirs()

            // Copy debug logs into export folder
            writeLogs(exportFolder)

            // Copy preferences into export folder
            writePreferences(exportFolder)

            // Copy database file into export folder
            writeDatabase(exportFolder)

            exportFolder
        } catch (e: Exception) {
            LogBuffer.e(TAG, "Database export prepare failed with error: ${e.message}")
            null
        }
    }

    private fun writeLogs(
        exportFolder: File,
    ) {
        try {
            val inputFile = File(File(context.filesDir, "logs"), "debug.log")
            val outputFile = File(exportFolder, "logs.txt")
            fileUtil.copy(src = inputFile, dst = outputFile)
        } catch (e: IOException) {
            Timber.e("Writing log file failed with error: $e")
        }
    }

    private fun writePreferences(
        exportFolder: File,
    ) {
        try {
            val outputFile = File(exportFolder, "preferences.xml")
            fileUtil.copyPreferences(sharedPrefs = sharedPrefs, outputFile = outputFile)
        } catch (e: IOException) {
            Timber.e("Writing preferences file failed with error: $e")
        }
    }

    private fun writeDatabase(
        exportFolder: File,
    ) {
        try {
            appDatabase.databaseFiles()
                ?.filter { it.exists() }
                ?.forEach { inputFile ->
                    val outputFile = File(exportFolder, inputFile.name)
                    fileUtil.copy(src = inputFile, dst = outputFile)
                }
        } catch (e: IOException) {
            Timber.e("Writing database file failed with error: $e")
        }
    }

    fun cleanup(file: File) {
        try {
            if (file.isDirectory) {
                fileUtil.deleteDirectoryContents(file.path)
            } else {
                fileUtil.deleteFileByPath(file.path)
            }
        } catch (e: Exception) {
            Timber.e("Could not cleanup file: $e")
        }
    }
}
