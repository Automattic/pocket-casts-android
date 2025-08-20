package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import com.google.protobuf.int32Value
import com.google.protobuf.int64Value
import com.google.protobuf.stringValue
import com.pocketcasts.service.api.Record
import com.pocketcasts.service.api.record
import com.pocketcasts.service.api.syncUserDevice

internal class DeviceSync(
    private val statsManager: StatsManager,
    private val settings: Settings,
) {
    suspend fun syncStats() {
        statsManager.cacheMergedStats()
        statsManager.setSyncStatus(true)
    }

    fun incrementalData(): List<Record> {
        return if (statsManager.isSynced(settings) || statsManager.isEmpty) {
            emptyList()
        } else {
            listOf(
                record {
                    device = syncUserDevice {
                        deviceId = stringValue { value = settings.getUniqueDeviceId() }
                        deviceType = AndroidDeviceType
                        timeSilenceRemoval = int64Value { value = statsManager.timeSavedSilenceRemovalSecs }
                        timeSkipping = int64Value { value = statsManager.timeSavedSkippingSecs }
                        timeIntroSkipping = int64Value { value = statsManager.timeSavedSkippingIntroSecs }
                        timeVariableSpeed = int64Value { value = statsManager.timeSavedVariableSpeedSecs }
                        timeListened = int64Value { value = statsManager.totalListeningTimeSecs }
                        timesStartedAt = int64Value { value = statsManager.statsStartTimeSecs }
                    }
                },
            )
        }
    }
}

private val AndroidDeviceType = int32Value { value = 2 }
