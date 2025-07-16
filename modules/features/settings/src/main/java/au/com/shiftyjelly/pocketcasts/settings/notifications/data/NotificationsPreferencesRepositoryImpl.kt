package au.com.shiftyjelly.pocketcasts.settings.notifications.data

import android.content.Context
import android.media.RingtoneManager
import androidx.core.net.toUri
import au.com.shiftyjelly.pocketcasts.preferences.NotificationSound
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationAction
import au.com.shiftyjelly.pocketcasts.preferences.model.NotificationVibrateSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceCategory
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceType
import au.com.shiftyjelly.pocketcasts.settings.util.TextResource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal class NotificationsPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: Settings,
    private val podcastManager: PodcastManager,
    private val notificationFeaturesProvider: NotificationFeaturesProvider,
) : NotificationsPreferenceRepository {

    override suspend fun getPreferenceCategories(): List<NotificationPreferenceCategory> {
        return buildList {
            add(
                NotificationPreferenceCategory(
                    title = TextResource.fromStringId(LR.string.settings_notifications_new_episodes),
                    preferences = buildList {
                        val isEnabled = settings.notifyRefreshPodcast.flow.value
                        add(
                            NotificationPreferenceType.NotifyMeOnNewEpisodes(
                                title = TextResource.fromStringId(LR.string.settings_notification_notify_me),
                                isEnabled = isEnabled,
                            ),
                        )
                        if (isEnabled) {
                            add(
                                NotificationPreferenceType.NotifyOnThesePodcasts(
                                    title = TextResource.fromStringId(LR.string.settings_notification_choose_podcasts),
                                    displayValue = getPodcastsSummary() ?: TextResource.fromText(""),
                                ),
                            )
                            add(
                                NotificationPreferenceType.NotificationActions(
                                    title = TextResource.fromStringId(LR.string.settings_notification_actions_title),
                                    value = settings.newEpisodeNotificationActions.value,
                                    options = NewEpisodeNotificationAction.entries,
                                    displayValue = getActionsSummary(),
                                ),
                            )

                            if (notificationFeaturesProvider.hasNotificationChannels) {
                                add(
                                    NotificationPreferenceType.AdvancedSettings(
                                        title = TextResource.fromStringId(LR.string.settings_notification_advanced),
                                        description = TextResource.fromStringId(LR.string.settings_notification_advanced_summary),
                                    ),
                                )
                            } else {
                                add(
                                    NotificationPreferenceType.NotificationSoundPreference(
                                        title = TextResource.fromStringId(LR.string.settings_notification_sound),
                                        notificationSound = settings.notificationSound.value,
                                        displayedSoundName = getNotificationSoundSummary(),
                                    ),
                                )
                                add(
                                    NotificationPreferenceType.NotificationVibration(
                                        title = TextResource.fromStringId(LR.string.settings_notification_vibrate),
                                        value = settings.notificationVibrate.value,
                                        options = listOf(
                                            NotificationVibrateSetting.NewEpisodes,
                                            NotificationVibrateSetting.OnlyWhenSilent,
                                            NotificationVibrateSetting.Never,
                                        ),
                                        displayValue = TextResource.fromStringId(settings.notificationVibrate.value.summary),
                                    ),
                                )
                            }
                        }
                    },
                ),
            )
            if (notificationFeaturesProvider.isRevampFeatureEnabled) {
                add(
                    NotificationPreferenceCategory(
                        title = TextResource.fromStringId(LR.string.settings_notification_recommendations),
                        preferences = buildList {
                            add(
                                NotificationPreferenceType.EnableRecommendations(
                                    title = TextResource.fromStringId(LR.string.settings_notification_notify_me),
                                    isEnabled = settings.recommendationsNotification.value,
                                ),
                            )
                            if (settings.recommendationsNotification.value && notificationFeaturesProvider.hasNotificationChannels) {
                                add(
                                    NotificationPreferenceType.RecommendationSettings(
                                        title = TextResource.fromStringId(LR.string.settings_notification_advanced),
                                        description = TextResource.fromStringId(LR.string.settings_notification_advanced_summary),
                                    ),
                                )
                            }
                        },
                    ),
                )

                add(
                    NotificationPreferenceCategory(
                        title = TextResource.fromStringId(LR.string.settings_notification_daily_reminders),
                        preferences = buildList {
                            add(
                                NotificationPreferenceType.EnableDailyReminders(
                                    title = TextResource.fromStringId(LR.string.settings_notification_notify_me),
                                    isEnabled = settings.dailyRemindersNotification.value,
                                ),
                            )
                            if (settings.dailyRemindersNotification.value && notificationFeaturesProvider.hasNotificationChannels) {
                                add(
                                    NotificationPreferenceType.DailyReminderSettings(
                                        title = TextResource.fromStringId(LR.string.settings_notification_advanced),
                                        description = TextResource.fromStringId(LR.string.settings_notification_advanced_summary),
                                    ),
                                )
                            }
                        },
                    ),
                )

                add(
                    NotificationPreferenceCategory(
                        title = TextResource.fromStringId(LR.string.settings_notification_new_features),
                        preferences = buildList {
                            add(
                                NotificationPreferenceType.EnableNewFeaturesAndTips(
                                    title = TextResource.fromStringId(LR.string.settings_notification_notify_me),
                                    isEnabled = settings.newFeaturesNotification.value,
                                ),
                            )
                            if (settings.newFeaturesNotification.value && notificationFeaturesProvider.hasNotificationChannels) {
                                add(
                                    NotificationPreferenceType.NewFeaturesAndTipsSettings(
                                        title = TextResource.fromStringId(LR.string.settings_notification_advanced),
                                        description = TextResource.fromStringId(LR.string.settings_notification_advanced_summary),
                                    ),
                                )
                            }
                        },
                    ),
                )

                add(
                    NotificationPreferenceCategory(
                        title = TextResource.fromStringId(LR.string.settings_notification_offers),
                        preferences = buildList {
                            add(
                                NotificationPreferenceType.EnableOffers(
                                    title = TextResource.fromStringId(LR.string.settings_notification_notify_me),
                                    isEnabled = settings.offersNotification.value,
                                ),
                            )
                            if (settings.offersNotification.value && notificationFeaturesProvider.hasNotificationChannels) {
                                add(
                                    NotificationPreferenceType.OffersSettings(
                                        title = TextResource.fromStringId(LR.string.settings_notification_advanced),
                                        description = TextResource.fromStringId(LR.string.settings_notification_advanced_summary),
                                    ),
                                )
                            }
                        },
                    ),
                )
            }
            add(
                NotificationPreferenceCategory(
                    title = TextResource.fromStringId(LR.string.settings),
                    preferences = listOf(
                        NotificationPreferenceType.PlayOverNotifications(
                            title = TextResource.fromStringId(LR.string.settings_notification_play_over),
                            value = settings.playOverNotification.value,
                            options = listOf(
                                PlayOverNotificationSetting.NEVER,
                                PlayOverNotificationSetting.DUCK,
                                PlayOverNotificationSetting.ALWAYS,
                            ),
                            displayValue = TextResource.fromStringId(settings.playOverNotification.value.titleRes),
                        ),
                        NotificationPreferenceType.HidePlaybackNotificationOnPause(
                            title = TextResource.fromStringId(LR.string.settings_notification_hide_on_pause),
                            isEnabled = settings.hideNotificationOnPause.value,
                        ),
                    ),
                ),
            )
        }
    }

    private suspend fun getPodcastsSummary() = podcastManager.findSubscribedFlow().map { podcasts ->
        val podcastCount = podcasts.size
        val notificationCount = podcasts.count { it.isShowNotifications }

        when {
            notificationCount == 0 -> TextResource.fromStringId(LR.string.settings_podcasts_selected_zero)
            notificationCount == 1 -> TextResource.fromStringId(LR.string.settings_podcasts_selected_one)
            notificationCount >= podcastCount -> TextResource.fromStringId(LR.string.settings_podcasts_selected_all)
            else -> TextResource.fromStringId(
                LR.string.settings_podcasts_selected_x,
                notificationCount,
            )
        }
    }.firstOrNull()

    private fun getActionsSummary(): String {
        val userActions = settings.newEpisodeNotificationActions.value
        val actionStrings = userActions.joinToString { context.getString(it.labelId) }
        return if (userActions.isEmpty()) context.getString(LR.string.none) else actionStrings
    }

    private fun getNotificationSoundSummary() = getRingtoneValue(settings.notificationSound.value.path)

    private fun getRingtoneValue(ringtonePath: String?): String {
        if (ringtonePath.isNullOrBlank()) {
            return context.getString(LR.string.settings_notification_silent)
        }
        return when (val ringtone = RingtoneManager.getRingtone(context, ringtonePath.toUri())) {
            null -> ""
            else -> {
                val title = ringtone.getTitle(context)
                if (title == NotificationSound.DEFAULT_SOUND) {
                    context.getString(LR.string.settings_notification_default_sound)
                } else {
                    title
                }
            }
        }
    }

    override suspend fun setPreference(preference: NotificationPreferenceType) {
        when (preference) {
            is NotificationPreferenceType.NotifyMeOnNewEpisodes -> {
                val enabled = preference.isEnabled
                settings.notifyRefreshPodcast.set(value = enabled, updateModifiedAt = true)
                podcastManager.updateAllShowNotifications(enabled)
                if (enabled) {
                    settings.setNotificationLastSeenToNow()
                }
            }

            is NotificationPreferenceType.HidePlaybackNotificationOnPause -> {
                settings.hideNotificationOnPause.set(value = preference.isEnabled, updateModifiedAt = true)
            }

            is NotificationPreferenceType.PlayOverNotifications -> {
                settings.playOverNotification.set(value = preference.value, updateModifiedAt = true)
            }

            is NotificationPreferenceType.NotificationVibration -> {
                settings.notificationVibrate.set(value = preference.value, updateModifiedAt = false)
            }

            is NotificationPreferenceType.NotificationActions -> {
                settings.newEpisodeNotificationActions.set(value = preference.value, updateModifiedAt = true)
            }

            is NotificationPreferenceType.NotificationSoundPreference -> {
                settings.notificationSound.set(value = preference.notificationSound, updateModifiedAt = false)
            }

            is NotificationPreferenceType.EnableDailyReminders -> {
                settings.dailyRemindersNotification.set(value = preference.isEnabled, updateModifiedAt = true)
            }

            is NotificationPreferenceType.EnableRecommendations -> {
                settings.recommendationsNotification.set(value = preference.isEnabled, updateModifiedAt = true)
            }

            is NotificationPreferenceType.EnableNewFeaturesAndTips -> {
                settings.newFeaturesNotification.set(value = preference.isEnabled, updateModifiedAt = true)
            }

            is NotificationPreferenceType.EnableOffers -> {
                settings.offersNotification.set(value = preference.isEnabled, updateModifiedAt = true)
            }

            is NotificationPreferenceType.OffersSettings,
            is NotificationPreferenceType.NewFeaturesAndTipsSettings,
            is NotificationPreferenceType.RecommendationSettings,
            is NotificationPreferenceType.DailyReminderSettings,
            is NotificationPreferenceType.AdvancedSettings,
            is NotificationPreferenceType.NotifyOnThesePodcasts,
            -> Unit
        }
    }
}
