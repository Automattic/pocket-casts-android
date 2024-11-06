package au.com.shiftyjelly.pocketcasts.utils

private const val lowStorageThreshold = 0.10

internal class LowStorageDetector {

    fun isRunningOnLowStorage(totalStorage: Long, availableStorage: Long): Boolean {
        return availableStorage < totalStorage * lowStorageThreshold
    }
}
