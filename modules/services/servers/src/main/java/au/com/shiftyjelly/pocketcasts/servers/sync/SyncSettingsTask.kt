package au.com.shiftyjelly.pocketcasts.servers.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import timber.log.Timber

class SyncSettingsTask(val context: Context, val parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    companion object {
        suspend fun run(settings: Settings, namedSettingsCall: NamedSettingsCaller): Result {
            try {
                val request = NamedSettingsRequest(
                    settings = NamedSettingsSettings(
                        skipForward = settings.skipForwardInSecs.getSyncValue(),
                        skipBack = if (settings.getSkipBackNeedsSync()) settings.getSkipBackwardInSecs() else null,
                        marketingOptIn = if (settings.getMarketingOptInNeedsSync()) settings.getMarketingOptIn() else null,
                        freeGiftAcknowledged = if (settings.getFreeGiftAcknowledgedNeedsSync()) settings.getFreeGiftAcknowledged() else null,
                        gridOrder = if (settings.getPodcastsSortTypeNeedsSync()) settings.getPodcastsSortType().serverId else null,
                    )
                )

                val response = namedSettingsCall.namedSettings(request)
                for ((key, value) in response) {
                    if (value.changed) {
                        Timber.d("$key changed to ${value.value}")

                        if (value.value is Number) { // Probably will have to change this when we do other settings, but for now just Number is fine
                            when (key) {
                                "skipForward" -> settings.skipForwardInSecs.set(value.value.toInt())
                                "skipBack" -> settings.setSkipBackwardInSec(value.value.toInt())
                                "gridOrder" -> settings.setPodcastsSortType(sortType = PodcastsSortType.fromServerId(value.value.toInt()), sync = false)
                            }
                        } else if (value.value is Boolean) {
                            when (key) {
                                "marketingOptIn" -> settings.setMarketingOptIn(value.value)
                                "freeGiftAcknowledgement" -> settings.setFreeGiftAcknowledged(value.value)
                            }
                        }
                    } else {
                        Timber.d("$key not changed")
                    }
                }
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Sync settings failed")
                return Result.failure()
            }

            settings.setSkipBackNeedsSync(false)
            settings.skipForwardInSecs.needsSync = false
            settings.setMarketingOptInNeedsSync(false)
            settings.setFreeGiftAcknowledgedNeedsSync(false)
            settings.setPodcastsSortTypeNeedsSync(false)

            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Settings synced")

            return Result.success()
        }
    }

    lateinit var settings: Settings
    lateinit var namedSettingsCaller: NamedSettingsCaller

    override suspend fun doWork(): Result {
        return run(settings, namedSettingsCaller)
    }
}
