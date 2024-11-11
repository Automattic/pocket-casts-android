package au.com.shiftyjelly.pocketcasts.utils

import android.os.Environment
import android.os.StatFs
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

suspend fun isDeviceRunningOnLowStorage(file: File = Environment.getExternalStorageDirectory()): Boolean = withContext(Dispatchers.IO) {
    try {
        val statFs = StatFs(file.path)
        val totalStorage: Long = statFs.blockCountLong * statFs.blockSizeLong
        val availableStorage: Long = statFs.availableBlocksLong * statFs.blockSizeLong

        LowStorageDetector().isRunningOnLowStorage(totalStorage, availableStorage)
    } catch (e: Exception) {
        Timber.e(e, "Error checking if device is running low on storage")
        false
    }
}
