package au.com.shiftyjelly.pocketcasts.servers.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.time.Instant
import timber.log.Timber

class SyncSettingsTask(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    companion object {
        suspend fun run(
            settings: Settings,
            lastSyncTime: Instant,
            namedSettingsCall: NamedSettingsCaller,
        ): Result {
            try {
                syncSettings(settings, lastSyncTime, namedSettingsCall)
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Sync settings failed")
                return Result.failure()
            }

            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Settings synced")

            return Result.success()
        }

        private suspend fun syncSettings(
            settings: Settings,
            lastSyncTime: Instant,
            namedSettingsCall: NamedSettingsCaller,
        ) {
            val request = NamedSettingsRequest(
                settings = NamedSettingsSettings(
                    skipForward = settings.skipForwardInSecs.getSyncValue(lastSyncTime),
                    skipBack = settings.skipBackInSecs.getSyncValue(lastSyncTime),
                    marketingOptIn = settings.marketingOptIn.getSyncValue(lastSyncTime),
                    freeGiftAcknowledged = settings.freeGiftAcknowledged.getSyncValue(lastSyncTime),
                    gridOrder = settings.podcastsSortType.getSyncValue(lastSyncTime)?.serverId,
                ),
            )

            val response = namedSettingsCall.namedSettings(request)
            for ((key, value) in response) {
                if (value.changed) {
                    Timber.d("$key changed to ${value.value}")

                    if (value.value is Number) { // Probably will have to change this when we do other settings, but for now just Number is fine
                        when (key) {
                            "skipForward" -> settings.skipForwardInSecs.set(value.value.toInt(), updateModifiedAt = false)
                            "skipBack" -> settings.skipBackInSecs.set(value.value.toInt(), updateModifiedAt = false)
                            "gridOrder" -> {
                                val sortType = PodcastsSortType.fromServerId(value.value.toInt())
                                settings.podcastsSortType.set(sortType, updateModifiedAt = false)
                            }
                        }
                    } else if (value.value is Boolean) {
                        when (key) {
                            "marketingOptIn" -> settings.marketingOptIn.set(value.value, updateModifiedAt = false)
                            "freeGiftAcknowledgement" -> settings.freeGiftAcknowledged.set(value.value, updateModifiedAt = false)
                        }
                    }
                } else {
                    Timber.d("$key not changed")
                }
            }
        }
    }

    lateinit var settings: Settings
    lateinit var namedSettingsCaller: NamedSettingsCaller

    override suspend fun doWork(): Result {
        val lastSyncTimeString = settings.getLastModified()
        val lastSyncTime = runCatching { Instant.parse(lastSyncTimeString) }
            .onFailure { Timber.e(it, "Could not convert lastModified String to Long: $lastSyncTimeString") }
            .getOrDefault(Instant.EPOCH)
        return run(settings, lastSyncTime, namedSettingsCaller)
    }
}
