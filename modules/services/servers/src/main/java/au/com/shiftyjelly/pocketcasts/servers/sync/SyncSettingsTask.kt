package au.com.shiftyjelly.pocketcasts.servers.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.helper.BuildConfig
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoAddUpNextLimitBehaviour
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoArchiveAfterPlayingSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoArchiveInactiveSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.preferences.model.ThemeSetting
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.lang.RuntimeException
import java.time.Instant
import java.time.format.DateTimeParseException
import timber.log.Timber

class SyncSettingsTask(val context: Context, val parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    companion object {
        suspend fun run(
            context: Context,
            settings: Settings,
            namedSettingsCall: NamedSettingsCaller,
        ): Result {
            try {
                if (FeatureFlag.isEnabled(Feature.SETTINGS_SYNC)) {
                    syncSettings(context, settings, namedSettingsCall)
                } else {
                    @Suppress("DEPRECATION")
                    oldSyncSettings(settings, namedSettingsCall)
                }
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Sync settings failed")
                return Result.failure()
            }

            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Settings synced")

            return Result.success()
        }

        private suspend fun syncSettings(
            context: Context,
            settings: Settings,
            namedSettingsCall: NamedSettingsCaller,
        ) {
            if (!FeatureFlag.isEnabled(Feature.SETTINGS_SYNC)) {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "syncSettings method should never be called if settings sync flag is not enabled")
                if (BuildConfig.DEBUG) throw RuntimeException("syncSettings method should never be called if settings sync flag is not enabled")
                return
            }

            val request = changedNamedSettingsRequest(context, settings)
            val response = namedSettingsCall.changedNamedSettings(request)
            processChangedNameSettingsResponse(context, settings, response)
        }

        private fun changedNamedSettingsRequest(
            context: Context,
            settings: Settings,
        ) = ChangedNamedSettingsRequest(
            changedSettings = ChangedNamedSettings(
                autoArchiveAfterPlaying = settings.autoArchiveAfterPlaying.getSyncSetting { autoArchiveAfterPlaying, modifiedAt ->
                    NamedChangedSettingInt(
                        value = autoArchiveAfterPlaying.toIndex(),
                        modifiedAt = modifiedAt,
                    )
                },
                autoArchiveInactive = settings.autoArchiveInactive.getSyncSetting { autoArchiveInactiveSetting, modifiedAt ->
                    NamedChangedSettingInt(
                        value = autoArchiveInactiveSetting.toIndex(),
                        modifiedAt = modifiedAt,
                    )
                },
                autoArchiveIncludesStarred = settings.autoArchiveIncludesStarred.getSyncSetting(::NamedChangedSettingBool),
                freeGiftAcknowledgement = settings.freeGiftAcknowledged.getSyncSetting(::NamedChangedSettingBool),
                gridOrder = settings.podcastsSortType.getSyncSetting { podcastSortType, modifiedAt ->
                    NamedChangedSettingInt(
                        value = podcastSortType.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                gridLayout = settings.podcastGridLayout.getSyncSetting { type, modifiedAt ->
                    NamedChangedSettingInt(
                        value = type.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                marketingOptIn = settings.marketingOptIn.getSyncSetting(::NamedChangedSettingBool),
                skipBack = settings.skipBackInSecs.getSyncSetting(::NamedChangedSettingInt),
                skipForward = settings.skipForwardInSecs.getSyncSetting(::NamedChangedSettingInt),
                playbackSpeed = settings.globalPlaybackEffects.getSyncSetting { effects, modifiedAt ->
                    NamedChangedSettingDouble(effects.playbackSpeed, modifiedAt)
                },
                trimSilence = settings.globalPlaybackEffects.getSyncSetting { effects, modifiedAt ->
                    NamedChangedSettingInt(effects.trimMode.serverId, modifiedAt)
                },
                volumeBoost = settings.globalPlaybackEffects.getSyncSetting { effects, modifiedAt ->
                    NamedChangedSettingBool(effects.isVolumeBoosted, modifiedAt)
                },
                rowAction = settings.streamingMode.getSyncSetting { mode, modifiedAt ->
                    NamedChangedSettingInt(
                        value = if (mode) 0 else 1,
                        modifiedAt = modifiedAt,
                    )
                },
                upNextSwipe = settings.upNextSwipe.getSyncSetting { action, modifiedAt ->
                    NamedChangedSettingInt(
                        value = action.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                episodeGrouping = settings.podcastGroupingDefault.getSyncSetting { type, modifiedAt ->
                    NamedChangedSettingInt(
                        value = type.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                showCustomMediaActions = settings.customMediaActionsVisibility.getSyncSetting(::NamedChangedSettingBool),
                mediaActionsOrder = settings.mediaControlItems.getSyncSetting { items, modifiedAt ->
                    NamedChangedSettingString(
                        value = items.joinToString(separator = ",", transform = Settings.MediaNotificationControls::serverId),
                        modifiedAt = modifiedAt,
                    )
                },
                keepScreenAwake = settings.keepScreenAwake.getSyncSetting(::NamedChangedSettingBool),
                openPlayerAutomatically = settings.openPlayerAutomatically.getSyncSetting(::NamedChangedSettingBool),
                showArchived = settings.showArchivedDefault.getSyncSetting(::NamedChangedSettingBool),
                intelligentResumption = settings.intelligentPlaybackResumption.getSyncSetting(::NamedChangedSettingBool),
                autoPlayEnabled = settings.autoPlayNextEpisodeOnEmpty.getSyncSetting(::NamedChangedSettingBool),
                hideNotificationOnPause = settings.hideNotificationOnPause.getSyncSetting(::NamedChangedSettingBool),
                playUpNextOnTap = settings.tapOnUpNextShouldPlay.getSyncSetting(::NamedChangedSettingBool),
                playOverNotifications = settings.playOverNotification.getSyncSetting { mode, modifiedAt ->
                    NamedChangedSettingInt(
                        value = mode.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                autoUpNextLimit = settings.autoAddUpNextLimit.getSyncSetting(::NamedChangedSettingInt),
                autoUpNextLimitReached = settings.autoAddUpNextLimitBehaviour.getSyncSetting { behaviour, modifiedAt ->
                    NamedChangedSettingInt(
                        value = behaviour.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                warnDataUsage = settings.warnOnMeteredNetwork.getSyncSetting(::NamedChangedSettingBool),
                showPodcastNotifications = settings.notifyRefreshPodcast.getSyncSetting(::NamedChangedSettingBool),
                collectAnalytics = settings.collectAnalytics.getSyncSetting(::NamedChangedSettingBool),
                sendCrashReports = settings.sendCrashReports.getSyncSetting(::NamedChangedSettingBool),
                linkCrashReportsToUser = settings.linkCrashReportsToUser.getSyncSetting(::NamedChangedSettingBool),
                addFileToUpNextAutomatically = settings.cloudAddToUpNext.getSyncSetting(::NamedChangedSettingBool),
                theme = settings.theme.getSyncSetting { value, modifiedAt ->
                    NamedChangedSettingInt(
                        value = value.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                podcastBadges = settings.podcastBadgeType.getSyncSetting { type, modifiedAt ->
                    NamedChangedSettingInt(
                        value = type.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                autoShowPlayed = settings.autoShowPlayed.takeIf { Util.isAutomotive(context) }?.getSyncSetting(::NamedChangedSettingBool),
                autoSubscribeToPlayed = settings.autoSubscribeToPlayed.takeIf { Util.isAutomotive(context) }?.getSyncSetting(::NamedChangedSettingBool),
            ),
        )

        private fun processChangedNameSettingsResponse(
            context: Context,
            settings: Settings,
            response: ChangedNamedSettingsResponse,
        ) {
            for ((key, changedSettingResponse) in response) {
                when (key) {
                    "autoArchiveInactive" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.autoArchiveInactive,
                        newSettingValue = run {
                            val index = (changedSettingResponse.value as? Number)?.toInt()
                            index?.let { AutoArchiveInactiveSetting.fromIndex(it) }
                        },
                    )
                    "autoArchiveIncludesStarred" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.autoArchiveIncludesStarred,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "autoArchivePlayed" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.autoArchiveAfterPlaying,
                        newSettingValue = run {
                            val index = (changedSettingResponse.value as? Number)?.toInt()
                            index?.let { AutoArchiveAfterPlayingSetting.fromIndex(it) }
                        },
                    )
                    "freeGiftAcknowledgement" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.freeGiftAcknowledged,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "gridOrder" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.podcastsSortType,
                        newSettingValue = run {
                            val serverId = (changedSettingResponse.value as? Number)?.toInt()
                            PodcastsSortType.fromServerId(serverId)
                        },
                    )
                    "gridLayout" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.podcastGridLayout,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let(PodcastGridLayoutType::fromServerId),
                    )
                    "marketingOptIn" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.marketingOptIn,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "skipBack" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.skipBackInSecs,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt(),
                    )
                    "skipForward" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.skipForwardInSecs,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt(),
                    )
                    "playbackSpeed" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.globalPlaybackEffects,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toDouble()?.let { newValue ->
                            settings.globalPlaybackEffects.value.apply {
                                playbackSpeed = newValue
                            }
                        },
                    )
                    "trimSilence" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.globalPlaybackEffects,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let { newValue ->
                            settings.globalPlaybackEffects.value.apply {
                                trimMode = TrimMode.fromServerId(newValue)
                            }
                        },
                    )
                    "volumeBoost" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.globalPlaybackEffects,
                        newSettingValue = (changedSettingResponse.value as? Boolean)?.let { newValue ->
                            settings.globalPlaybackEffects.value.apply {
                                isVolumeBoosted = newValue
                            }
                        },
                    )
                    "rowAction" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.streamingMode,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let { it == 0 },
                    )
                    "upNextSwipe" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.upNextSwipe,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let(Settings.UpNextAction::fromServerId),
                    )
                    "mediaActions" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.customMediaActionsVisibility,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "mediaActionsOrder" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.mediaControlItems,
                        newSettingValue = (changedSettingResponse.value as? String)
                            ?.split(',')
                            ?.mapNotNull(Settings.MediaNotificationControls::fromServerId)
                            ?.appendMissingControls(),
                    )
                    "episodeGrouping" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.podcastGroupingDefault,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let(PodcastGrouping::fromServerId),
                    )
                    "keepScreenAwake" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.keepScreenAwake,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "openPlayer" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.openPlayerAutomatically,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "showArchived" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.showArchivedDefault,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "intelligentResumption" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.intelligentPlaybackResumption,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "autoPlayEnabled" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.autoPlayNextEpisodeOnEmpty,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "hideNotificationOnPause" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.hideNotificationOnPause,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "playUpNextOnTap" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.tapOnUpNextShouldPlay,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "playOverNotifications" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.playOverNotification,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let(PlayOverNotificationSetting::fromServerId),
                    )
                    "autoUpNextLimit" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.autoAddUpNextLimit,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt(),
                    )
                    "autoUpNextLimitReached" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.autoAddUpNextLimitBehaviour,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let(AutoAddUpNextLimitBehaviour::fromServerId),
                    )
                    "warnDataUsage" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.warnOnMeteredNetwork,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "notifications" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.notifyRefreshPodcast,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "privacyAnalytics" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.collectAnalytics,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "privacyCrashReports" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.sendCrashReports,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "privacyLinkAccount" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.linkCrashReportsToUser,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "filesAutoUpNext" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.cloudAddToUpNext,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "theme" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.theme,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let(ThemeSetting::fromServerId),
                    )
                    "badges" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.podcastBadgeType,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let(BadgeType::fromServerId),
                    )
                    "autoShowPlayed" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.autoShowPlayed,
                        newSettingValue = (changedSettingResponse.value as? Boolean)?.takeIf { Util.isAutomotive(context = context) },
                    )
                    "autoSubscribeToPlayed" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.autoSubscribeToPlayed,
                        newSettingValue = (changedSettingResponse.value as? Boolean)?.takeIf { Util.isAutomotive(context = context) },
                    )
                    else -> LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Cannot handle named setting response with unknown key: $key")
                }
            }
        }

        private fun <T> updateSettingIfPossible(
            changedSettingResponse: ChangedSettingResponse,
            setting: UserSetting<T>,
            newSettingValue: T?,
        ) {
            if (newSettingValue == null) {
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
                value = newSettingValue,
                needsSync = false,
            )
        }

        @Suppress("DEPRECATION")
        @Deprecated("This can be removed when Feature.SETTINGS_SYNC flag is removed")
        private suspend fun oldSyncSettings(
            settings: Settings,
            namedSettingsCall: NamedSettingsCaller,
        ) {
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
                            "skipForward" -> settings.skipForwardInSecs.set(value.value.toInt(), needsSync = false)
                            "skipBack" -> settings.skipBackInSecs.set(value.value.toInt(), needsSync = false)
                            "gridOrder" -> {
                                val sortType = PodcastsSortType.fromServerId(value.value.toInt())
                                settings.podcastsSortType.set(sortType, needsSync = false)
                            }
                        }
                    } else if (value.value is Boolean) {
                        when (key) {
                            "marketingOptIn" -> settings.marketingOptIn.set(value.value, needsSync = false)
                            "freeGiftAcknowledgement" -> settings.freeGiftAcknowledged.set(value.value, needsSync = false)
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
        return run(context, settings, namedSettingsCaller)
    }
}

private fun List<Settings.MediaNotificationControls>.appendMissingControls() = this + (Settings.MediaNotificationControls.All - this)
