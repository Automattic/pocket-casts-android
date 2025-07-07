package au.com.shiftyjelly.pocketcasts.utils

private const val LOW_STORAGE_THRESHOLD = 0.10

internal class LowStorageDetector {

    fun isRunningOnLowStorage(totalStorage: Long, availableStorage: Long): Boolean {
        return availableStorage < totalStorage * LOW_STORAGE_THRESHOLD
    }
}
