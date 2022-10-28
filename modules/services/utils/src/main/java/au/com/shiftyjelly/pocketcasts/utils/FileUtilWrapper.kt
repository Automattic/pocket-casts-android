package au.com.shiftyjelly.pocketcasts.utils

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

class FileUtilWrapper @Inject constructor() {
    fun deleteDirectoryContents(path: String) {
        FileUtil.deleteDirectoryContents(path)
    }

    suspend fun saveBitmapToFile(
        image: Bitmap,
        context: Context,
        saveFolderName: String,
        saveFileName: String,
    ): File? = withContext(Dispatchers.IO) {
        val imagesFolder = File(context.cacheDir, saveFolderName)
        var file: File? = null
        try {
            imagesFolder.mkdirs()
            file = File(imagesFolder, saveFileName)
            val stream = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            Timber.e("Error while saving image to file " + e.message)
        }
        file
    }
}
