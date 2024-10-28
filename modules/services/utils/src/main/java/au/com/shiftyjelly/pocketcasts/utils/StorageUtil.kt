package au.com.shiftyjelly.pocketcasts.utils

import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val lowStorageThreshold = 0.10

suspend fun isDeviceRunningOnLowStorage(statFs: StatFs = StatFs(Environment.getExternalStorageDirectory().path)): Boolean = withContext(Dispatchers.IO) {
    try {
        val totalStorage = statFs.blockCountLong * statFs.blockSizeLong
        val availableStorage = statFs.availableBlocksLong * statFs.blockSizeLong

        availableStorage < totalStorage * lowStorageThreshold
    } catch (e: Exception) {
        Timber.e(e, "Error checking if device is running low on storage")
        false
    }
}
