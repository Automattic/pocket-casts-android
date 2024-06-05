package au.com.shiftyjelly.pocketcasts.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.Xml
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class FileUtilWrapper @Inject constructor() {
    fun deleteFileByPath(path: String) {
        FileUtil.deleteFileByPath(path)
    }

    fun deleteDirectoryContents(path: String) {
        FileUtil.deleteDirContents(path)
    }

    fun copy(src: File, dst: File) {
        FileUtil.copy(src, dst)
    }

    suspend fun saveBitmapToFile(
        bitmap: Bitmap,
        context: Context,
        saveFolderName: String,
        saveFileName: String,
    ): File? = withContext(Dispatchers.IO) {
        val imagesFolder = File(context.cacheDir, saveFolderName)
        var file: File? = null
        try {
            imagesFolder.mkdirs()
            file = File(imagesFolder, saveFileName)
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
                stream.flush()
            }
        } catch (e: IOException) {
            Timber.e("Error while saving image to file " + e.message)
        }
        file
    }

    // https://stackoverflow.com/a/63828765/193545
    fun zip(inputFolder: File, outputZipPath: File) = try {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipPath))).use { zos ->
            inputFolder.walkTopDown().forEach { file ->
                val zipFileName = file.absolutePath.removePrefix(inputFolder.absolutePath).removePrefix("/")
                val entry = ZipEntry("$zipFileName${(if (file.isDirectory) "/" else "")}")
                zos.putNextEntry(entry)
                if (file.isFile) {
                    file.inputStream().use { fis -> fis.copyTo(zos) }
                }
            }
        }
        true
    } catch (e: Exception) {
        Timber.e("Error while adding files to zip folder, ${e.message}")
        false
    }

    fun copyPreferences(
        sharedPrefs: SharedPreferences,
        outputFile: File,
    ) {
        var writer: FileWriter? = null
        try {
            writer = FileWriter(outputFile)

            val serializer = Xml.newSerializer()
            serializer.setOutput(writer)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", MAP)
            for ((key, value1) in sharedPrefs.all.entries) {
                val valueObject = value1 ?: continue
                val valueType = valueObject.javaClass.simpleName
                val value = valueObject.toString()
                serializer.startTag("", valueType)
                serializer.attribute("", NAME, key)
                serializer.text(value)
                serializer.endTag("", valueType)
            }
            serializer.endTag("", MAP)

            serializer.endDocument()
        } catch (e: Exception) {
            Timber.e(e.message)
        } finally {
            try {
                writer?.close()
            } catch (ignored: IOException) {
                // no-op
            }
        }
    }

    companion object {
        private const val NAME = "name"
        private const val MAP = "map"
    }
}
