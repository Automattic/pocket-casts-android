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
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeDefault
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeForPodcast
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationAction
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
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
            lastSyncTime: Instant,
            settings: Settings,
            namedSettingsCall: NamedSettingsCaller,
        ): Result {
            try {
                if (FeatureFlag.isEnabled(Feature.SETTINGS_SYNC)) {
                    syncSettings(context, settings, namedSettingsCall, lastSyncTime)
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
            lastSyncTime: Instant,
        ) {
            if (!FeatureFlag.isEnabled(Feature.SETTINGS_SYNC)) {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "syncSettings method should never be called if settings sync flag is not enabled")
                if (BuildConfig.DEBUG) throw RuntimeException("syncSettings method should never be called if settings sync flag is not enabled")
                return
            }

            val request = changedNamedSettingsRequest(context, lastSyncTime, settings)
            val response = namedSettingsCall.changedNamedSettings(request)
            processChangedNameSettingsResponse(context, settings, response)
        }

        private fun changedNamedSettingsRequest(
            context: Context,
            lastSyncTime: Instant,
            settings: Settings,
        ) = ChangedNamedSettingsRequest(
            changedSettings = ChangedNamedSettings(
                autoArchiveAfterPlaying = settings.autoArchiveAfterPlaying.getSyncSetting(lastSyncTime) { autoArchiveAfterPlaying, modifiedAt ->
                    NamedChangedSettingInt(
                        value = autoArchiveAfterPlaying.toIndex(),
                        modifiedAt = modifiedAt,
                    )
                },
                autoArchiveInactive = settings.autoArchiveInactive.getSyncSetting(lastSyncTime) { autoArchiveInactiveSetting, modifiedAt ->
                    NamedChangedSettingInt(
                        value = autoArchiveInactiveSetting.toIndex(),
                        modifiedAt = modifiedAt,
                    )
                },
                autoArchiveIncludesStarred = settings.autoArchiveIncludesStarred.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                freeGiftAcknowledgement = settings.freeGiftAcknowledged.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                gridOrder = settings.podcastsSortType.getSyncSetting(lastSyncTime) { podcastSortType, modifiedAt ->
                    NamedChangedSettingInt(
                        value = podcastSortType.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                gridLayout = settings.podcastGridLayout.getSyncSetting(lastSyncTime) { type, modifiedAt ->
                    NamedChangedSettingInt(
                        value = type.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                marketingOptIn = settings.marketingOptIn.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                skipBack = settings.skipBackInSecs.getSyncSetting(lastSyncTime, ::NamedChangedSettingInt),
                skipForward = settings.skipForwardInSecs.getSyncSetting(lastSyncTime, ::NamedChangedSettingInt),
                playbackSpeed = settings.globalPlaybackEffects.getSyncSetting(lastSyncTime) { effects, modifiedAt ->
                    NamedChangedSettingDouble(effects.playbackSpeed, modifiedAt)
                },
                trimSilence = settings.globalPlaybackEffects.getSyncSetting(lastSyncTime) { effects, modifiedAt ->
                    NamedChangedSettingInt(effects.trimMode.serverId, modifiedAt)
                },
                volumeBoost = settings.globalPlaybackEffects.getSyncSetting(lastSyncTime) { effects, modifiedAt ->
                    NamedChangedSettingBool(effects.isVolumeBoosted, modifiedAt)
                },
                rowAction = settings.streamingMode.getSyncSetting(lastSyncTime) { mode, modifiedAt ->
                    NamedChangedSettingInt(
                        value = if (mode) 0 else 1,
                        modifiedAt = modifiedAt,
                    )
                },
                upNextSwipe = settings.upNextSwipe.getSyncSetting(lastSyncTime) { action, modifiedAt ->
                    NamedChangedSettingInt(
                        value = action.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                episodeGrouping = settings.podcastGroupingDefault.getSyncSetting(lastSyncTime) { type, modifiedAt ->
                    NamedChangedSettingInt(
                        value = type.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                showCustomMediaActions = settings.customMediaActionsVisibility.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                mediaActionsOrder = settings.mediaControlItems.getSyncSetting(lastSyncTime) { items, modifiedAt ->
                    NamedChangedSettingString(
                        value = items.joinToString(separator = ",", transform = Settings.MediaNotificationControls::serverId),
                        modifiedAt = modifiedAt,
                    )
                },
                keepScreenAwake = settings.keepScreenAwake.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                openPlayerAutomatically = settings.openPlayerAutomatically.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                showArchived = settings.showArchivedDefault.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                intelligentResumption = settings.intelligentPlaybackResumption.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                autoPlayEnabled = settings.autoPlayNextEpisodeOnEmpty.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                hideNotificationOnPause = settings.hideNotificationOnPause.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                playUpNextOnTap = settings.tapOnUpNextShouldPlay.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                playOverNotifications = settings.playOverNotification.getSyncSetting(lastSyncTime) { mode, modifiedAt ->
                    NamedChangedSettingInt(
                        value = mode.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                autoUpNextLimit = settings.autoAddUpNextLimit.getSyncSetting(lastSyncTime, ::NamedChangedSettingInt),
                autoUpNextLimitReached = settings.autoAddUpNextLimitBehaviour.getSyncSetting(lastSyncTime) { behaviour, modifiedAt ->
                    NamedChangedSettingInt(
                        value = behaviour.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                warnDataUsage = settings.warnOnMeteredNetwork.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                showPodcastNotifications = settings.notifyRefreshPodcast.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                collectAnalytics = settings.collectAnalytics.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                sendCrashReports = settings.sendCrashReports.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                linkCrashReportsToUser = settings.linkCrashReportsToUser.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                addFileToUpNextAutomatically = settings.cloudAddToUpNext.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                theme = settings.theme.getSyncSetting(lastSyncTime) { value, modifiedAt ->
                    NamedChangedSettingInt(
                        value = value.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                podcastBadges = settings.podcastBadgeType.getSyncSetting(lastSyncTime) { type, modifiedAt ->
                    NamedChangedSettingInt(
                        value = type.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                autoShowPlayed = settings.autoShowPlayed.takeIf { Util.isAutomotive(context) }?.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                autoSubscribeToPlayed = settings.autoSubscribeToPlayed.takeIf { Util.isAutomotive(context) }?.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                autoPlayLastSource = settings.lastAutoPlaySource.getSyncSetting(lastSyncTime) { source, modifiedAt ->
                    NamedChangedSettingString(
                        value = source.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                useEmbeddedArtwork = settings.useEmbeddedArtwork.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                notificationSettingActions = settings.newEpisodeNotificationActions.getSyncSetting(lastSyncTime) { actions, modifiedAt ->
                    NamedChangedSettingString(
                        value = actions.joinToString(separator = ",", transform = NewEpisodeNotificationAction::serverId),
                        modifiedAt = modifiedAt,
                    )
                },
                playerShelfItems = settings.shelfItems.getSyncSetting(lastSyncTime) { items, modifiedAt ->
                    NamedChangedSettingString(
                        value = items.joinToString(separator = ",", transform = ShelfItem::serverId),
                        modifiedAt = modifiedAt,
                    )
                },
                showArtworkOnLockScreen = settings.showArtworkOnLockScreen.getSyncSetting(lastSyncTime) { setting, modifiedAt ->
                    NamedChangedSettingBool(
                        value = setting,
                        modifiedAt = modifiedAt,
                    )
                },
                headphoneControlsNextAction = settings.headphoneControlsNextAction.getSyncSetting(lastSyncTime) { setting, modifiedAt ->
                    NamedChangedSettingInt(
                        value = setting.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                headphoneControlsPreviousAction = settings.headphoneControlsPreviousAction.getSyncSetting(lastSyncTime) { setting, modifiedAt ->
                    NamedChangedSettingInt(
                        value = setting.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                headphoneControlsPlayBookmarkConfirmationSound = settings.headphoneControlsPlayBookmarkConfirmationSound.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                episodeBookmarksSortType = settings.episodeBookmarksSortType.getSyncSetting(lastSyncTime) { setting, modifiedAt ->
                    NamedChangedSettingInt(
                        value = setting.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                playerBookmarksSortType = settings.playerBookmarksSortType.getSyncSetting(lastSyncTime) { setting, modifiedAt ->
                    NamedChangedSettingInt(
                        value = setting.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                podcastBookmarksSortType = settings.podcastBookmarksSortType.getSyncSetting(lastSyncTime) { setting, modifiedAt ->
                    NamedChangedSettingInt(
                        value = setting.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                useDarkUpNextTheme = settings.useDarkUpNextTheme.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                useDynamicColorsForWidget = settings.useDynamicColorsForWidget.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
                filesSortOrder = settings.cloudSortOrder.getSyncSetting(lastSyncTime) { setting, modifiedAt ->
                    NamedChangedSettingInt(
                        value = setting.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                darkThemePreference = settings.darkThemePreference.getSyncSetting(lastSyncTime) { value, modifiedAt ->
                    NamedChangedSettingInt(
                        value = value.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                lightThemePreference = settings.lightThemePreference.getSyncSetting(lastSyncTime) { value, modifiedAt ->
                    NamedChangedSettingInt(
                        value = value.serverId,
                        modifiedAt = modifiedAt,
                    )
                },
                useSystemTheme = settings.useSystemTheme.getSyncSetting(lastSyncTime, ::NamedChangedSettingBool),
            ),
        )

        private fun processChangedNameSettingsResponse(
            context: Context,
            settings: Settings,
            response: ChangedNamedSettingsResponse,
        ) {
            var isThemeChanged = false

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
                    "useEmbeddedArtwork" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.useEmbeddedArtwork,
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
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let { ThemeSetting.fromServerId(it) ?: ThemeSetting.LIGHT },
                    ).also { isThemeChanged = it != null || isThemeChanged }
                    "darkThemePreference" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.darkThemePreference,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let { ThemeSetting.fromServerId(it) ?: ThemeSetting.DARK },
                    ).also { isThemeChanged = it != null || isThemeChanged }
                    "lightThemePreference" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.lightThemePreference,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let { ThemeSetting.fromServerId(it) ?: ThemeSetting.LIGHT },
                    ).also { isThemeChanged = it != null || isThemeChanged }
                    "useSystemTheme" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.useSystemTheme,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    ).also { isThemeChanged = it != null || isThemeChanged }
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
                    "autoPlayLastListUuid" -> {
                        val syncedValue = updateSettingIfPossible(
                            changedSettingResponse = changedSettingResponse,
                            setting = settings.lastAutoPlaySource,
                            newSettingValue = (changedSettingResponse.value as? String)?.let(AutoPlaySource::fromServerId),
                        )
                        if (syncedValue != null) {
                            settings.trackingAutoPlaySource.set(syncedValue, needsSync = false)
                        }
                    }
                    "notificationActions" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.newEpisodeNotificationActions,
                        newSettingValue = (changedSettingResponse.value as? String)
                            ?.split(',')
                            ?.mapNotNull(NewEpisodeNotificationAction::fromServerId),
                    )
                    "playerShelf" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.shelfItems,
                        newSettingValue = (changedSettingResponse.value as? String)
                            ?.split(',')
                            ?.mapNotNull(ShelfItem::fromServerId),
                    )
                    "showArtworkOnLockScreen" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.showArtworkOnLockScreen,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "headphoneControlsNextAction" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.headphoneControlsNextAction,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let { HeadphoneAction.fromServerId(it) ?: HeadphoneAction.SKIP_FORWARD },
                    )
                    "headphoneControlsPreviousAction" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.headphoneControlsPreviousAction,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let { HeadphoneAction.fromServerId(it) ?: HeadphoneAction.SKIP_BACK },
                    )
                    "headphoneControlsPlayBookmarkConfirmationSound" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.headphoneControlsPlayBookmarkConfirmationSound,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "episodeBookmarksSortType" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.episodeBookmarksSortType,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let { BookmarksSortTypeDefault.fromServerId(it) ?: BookmarksSortTypeDefault.DATE_ADDED_NEWEST_TO_OLDEST },
                    )
                    "playerBookmarksSortType" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.playerBookmarksSortType,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let { BookmarksSortTypeDefault.fromServerId(it) ?: BookmarksSortTypeDefault.DATE_ADDED_NEWEST_TO_OLDEST },
                    )
                    "podcastBookmarksSortType" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.podcastBookmarksSortType,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let { BookmarksSortTypeForPodcast.fromServerId(it) ?: BookmarksSortTypeForPodcast.DATE_ADDED_NEWEST_TO_OLDEST },
                    )
                    "useDarkUpNextTheme" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.useDarkUpNextTheme,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "useDynamicColorsForWidget" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.useDynamicColorsForWidget,
                        newSettingValue = (changedSettingResponse.value as? Boolean),
                    )
                    "filesSortOrder" -> updateSettingIfPossible(
                        changedSettingResponse = changedSettingResponse,
                        setting = settings.cloudSortOrder,
                        newSettingValue = (changedSettingResponse.value as? Number)?.toInt()?.let { Settings.CloudSortOrder.fromServerId(it) ?: Settings.CloudSortOrder.NEWEST_OLDEST },
                    )
                    else -> LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Cannot handle named setting response with unknown key: $key")
                }
            }

            if (isThemeChanged) {
                settings.requestThemeReconfiguration()
            }
        }

        private fun <T> updateSettingIfPossible(
            changedSettingResponse: ChangedSettingResponse,
            setting: UserSetting<T>,
            newSettingValue: T?,
        ): T? {
            if (newSettingValue == null) {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Invalid ${setting.sharedPrefKey} value: ${changedSettingResponse.value}")
                return null
            }

            if (changedSettingResponse.modifiedAt == null) {
                Timber.i("Not syncing ${setting.sharedPrefKey} from the server because setting was not modifiedAt on the server")
                return null
            }

            val serverModifiedAtInstant = try {
                Instant.parse(changedSettingResponse.modifiedAt)
            } catch (e: DateTimeParseException) {
                LogBuffer.e(
                    LogBuffer.TAG_INVALID_STATE,
                    "Not syncing ${setting.sharedPrefKey} from the server because server returned modifiedAt value that could not be parsed: ${changedSettingResponse.modifiedAt}",
                )
                return null
            }

            val localModifiedAt = setting.getModifiedAt()
            // Don't exit early if we don't have a local modifiedAt time since
            // we don't know the local value is newer than the server value.
            if (localModifiedAt != null && localModifiedAt.isAfter(serverModifiedAtInstant)) {
                Timber.i("Not syncing ${setting.sharedPrefKey} value of $newSettingValue from the server because setting was modified more recently locally")
                return null
            }

            setting.set(
                value = newSettingValue,
                needsSync = false,
            )
            return newSettingValue
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
        val lastSyncTime = runCatching { Instant.parse(settings.getLastModified()) }.getOrDefault(Instant.EPOCH)
        return run(context, lastSyncTime, settings, namedSettingsCaller)
    }
}

private fun List<Settings.MediaNotificationControls>.appendMissingControls() = this + (Settings.MediaNotificationControls.All - this)
