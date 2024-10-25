package au.com.shiftyjelly.pocketcasts.utils

import android.os.Environment
import android.os.StatFs

private const val lowStorageThreshold = 0.10

fun isDeviceRunningOnLowStorage(): Boolean {
    val statFs = StatFs(Environment.getExternalStorageDirectory().path)
    val totalStorage = statFs.blockCountLong * statFs.blockSizeLong
    val availableStorage = statFs.availableBlocksLong * statFs.blockSizeLong

    return availableStorage < totalStorage * lowStorageThreshold
}
