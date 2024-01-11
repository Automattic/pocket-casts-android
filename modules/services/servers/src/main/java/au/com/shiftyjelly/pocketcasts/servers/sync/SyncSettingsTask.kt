package au.com.shiftyjelly.pocketcasts.servers.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.helper.BuildConfig
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.lang.RuntimeException
import java.time.Instant
import java.time.format.DateTimeParseException
import timber.log.Timber

class SyncSettingsTask(val context: Context, val parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    companion object {
        suspend fun run(settings: Settings, namedSettingsCall: NamedSettingsCaller): Result {
            try {
                if (FeatureFlag.isEnabled(Feature.SETTINGS_SYNC)) {
                    syncSettings(settings, namedSettingsCall)
                } else {
                    @Suppress("DEPRECATION")
                    oldSyncSettings(settings, namedSettingsCall)
                }
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Sync settings failed")
                return Result.failure()
            }

            listOf(
                settings.freeGiftAcknowledged,
                settings.marketingOptIn,
                settings.podcastsSortType,
                settings.skipBackInSecs,
                settings.skipForwardInSecs,
            ).forEach {
                it.doesNotNeedSync()
            }
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Settings synced")

            return Result.success()
        }

        private suspend fun syncSettings(
            settings: Settings,
            namedSettingsCall: NamedSettingsCaller,
        ) {
            if (!FeatureFlag.isEnabled(Feature.SETTINGS_SYNC)) {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "syncSettings method should never be called if settings sync flag is not enabled")
                if (BuildConfig.DEBUG) throw RuntimeException("syncSettings method should never be called if settings sync flag is not enabled")
                return
            }

            val request = ChangedNamedSettingsRequest(
                changedSettings = ChangedNamedSettings(
                    skipForward = settings.skipForwardInSecs.getSyncSetting(::NamedChangedSettingInt),
                ),
            )
            val response = namedSettingsCall.changedNamedSettings(request)
            for ((key, value) in response) {
                when (key) {
                    "skipForward" -> updateSettingIfPossible(value, settings.skipForwardInSecs) {
                        (it.value as? Number)?.toInt()
                    }
                }
            }
        }

        private fun <T> updateSettingIfPossible(
            changedSettingResponse: ChangedSettingResponse,
            setting: UserSetting<T>,
            transformToValue: (ChangedSettingResponse) -> T?,
        ) {
            val value = transformToValue(changedSettingResponse)
            if (value == null) {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Invalid ${setting.sharedPrefKey} value: ${changedSettingResponse.value}")
                return
            }

            if (changedSettingResponse.modifiedAt == null) {
                Timber.i("Not syncing ${setting.sharedPrefKey} from the server because setting was not modifiedAt on the server")
                return
            }

            val serverModifiedAtInstant = try {
                Instant.parse(changedSettingResponse.modifiedAt)
            } catch (e: DateTimeParseException) {
                LogBuffer.e(
                    LogBuffer.TAG_INVALID_STATE,
                    "Not syncing ${setting.sharedPrefKey} from the server because server returned modifiedAt value that could not be parsed: ${changedSettingResponse.modifiedAt}",
                )
                return
            }

            val localModifiedAt = setting.getModifiedAt()
            // Don't exit early if we don't have a local modifiedAt time since
            // we don't know the local value is newer than the server value.
            if (localModifiedAt != null && localModifiedAt.isAfter(serverModifiedAtInstant)) {
                Timber.i("Not syncing ${setting.sharedPrefKey} value of $newSettingValue from the server because setting was modified more recently locally")
                return
            }

            setting.set(
                value = value,
                needsSync = false,
            )
        }

        @Suppress("DEPRECATION")
        @Deprecated("This can be removed when Feature.SETTINGS_SYNC flag is removed")
        private suspend fun oldSyncSettings(
            settings: Settings,
            namedSettingsCall: NamedSettingsCaller,
        ) {
            @Suppress("DEPRECATION")
            val request = NamedSettingsRequest(
                settings = NamedSettingsSettings(
                    skipForward = settings.skipForwardInSecs.getSyncValue(),
                    skipBack = settings.skipBackInSecs.getSyncValue(),
                    marketingOptIn = settings.marketingOptIn.getSyncValue(),
                    freeGiftAcknowledged = settings.freeGiftAcknowledged.getSyncValue(),
                    gridOrder = settings.podcastsSortType.getSyncValue()?.serverId,
                ),
            )

            val response = namedSettingsCall.namedSettings(request)
            for ((key, value) in response) {
                if (value.changed) {
                    Timber.d("$key changed to ${value.value}")

                    if (value.value is Number) { // Probably will have to change this when we do other settings, but for now just Number is fine
                        when (key) {
                            "skipForward" -> settings.skipForwardInSecs.set(value.value.toInt())
                            "skipBack" -> settings.skipBackInSecs.set(value.value.toInt())
                            "gridOrder" -> {
                                val sortType = PodcastsSortType.fromServerId(value.value.toInt())
                                settings.podcastsSortType.set(sortType)
                            }
                        }
                    } else if (value.value is Boolean) {
                        when (key) {
                            "marketingOptIn" -> settings.marketingOptIn.set(value.value)
                            "freeGiftAcknowledgement" -> settings.freeGiftAcknowledged.set(value.value)
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
        return run(settings, namedSettingsCaller)
    }
}
