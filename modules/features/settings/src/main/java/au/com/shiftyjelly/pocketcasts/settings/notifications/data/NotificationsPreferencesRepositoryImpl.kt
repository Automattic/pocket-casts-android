package au.com.shiftyjelly.pocketcasts.settings.notifications.data

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.net.toUri
import au.com.shiftyjelly.pocketcasts.preferences.NotificationSound
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationAction
import au.com.shiftyjelly.pocketcasts.preferences.model.NotificationVibrateSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceCategory
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal class NotificationsPreferencesRepositoryImpl @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
    private val settings: Settings,
    private val podcastManager: PodcastManager,
) : NotificationsPreferenceRepository {

    override suspend fun getPreferenceCategories(): List<NotificationPreferenceCategory> = withContext(dispatcher) {
        listOf(
            NotificationPreferenceCategory(
                title = context.getString(LR.string.settings_notifications_new_episodes),
                preferences = buildList {
                    val isEnabled = settings.notifyRefreshPodcast.flow.value
                    add(
                        NotificationPreference.SwitchPreference(
                            title = context.getString(LR.string.settings_notification_notify_me),
                            value = isEnabled,
                            preference = NotificationPreferences.NEW_EPISODES_NOTIFY_ME
                        )
                    )
                    if (isEnabled) {
                        add(
                            NotificationPreference.TextPreference(
                                title = context.getString(LR.string.settings_notification_choose_podcasts),
                                value = getPodcastsSummary(),
                                preference = NotificationPreferences.NEW_EPISODES_CHOOSE_PODCASTS
                            )
                        )
                        add(
                            NotificationPreference.MultiSelectPreference(
                                title = context.getString(LR.string.settings_notification_actions_title),
                                maxNumberOfSelectableOptions = 3,
                                value = settings.newEpisodeNotificationActions.value,
                                preference = NotificationPreferences.NEW_EPISODES_ACTIONS,
                                options = NewEpisodeNotificationAction.entries,
                                displayText = getActionsSummary(),
                            )
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            add(
                                NotificationPreference.TextPreference(
                                    title = context.getString(LR.string.settings_notification_advanced),
                                    value = context.getString(LR.string.settings_notification_advanced_summary),
                                    preference = NotificationPreferences.NEW_EPISODES_ADVANCED
                                )
                            )
//                        } else {
                            add(
                                NotificationPreference.ValueHolderPreference(
                                    title = context.getString(LR.string.settings_notification_sound),
                                    value = settings.notificationSound.value.path,
                                    displayValue = getNotificationSoundSummary(),
                                    preference = NotificationPreferences.NEW_EPISODES_RINGTONE
                                )
                            )
                            add(
                                NotificationPreference.RadioGroupPreference(
                                    title = context.getString(LR.string.settings_notification_vibrate),
                                    value = settings.notificationVibrate.value,
                                    options = listOf(
                                        NotificationVibrateSetting.NewEpisodes,
                                        NotificationVibrateSetting.OnlyWhenSilent,
                                        NotificationVibrateSetting.Never,
                                    ),
                                    preference =  NotificationPreferences.NEW_EPISODES_VIBRATION,
                                    displayText = context.getString(settings.notificationVibrate.value.summary)
                                )
                            )
//                        }
                    }
                }
            ),
            NotificationPreferenceCategory(
                title = context.getString(LR.string.settings),
                preferences = listOf(
                    NotificationPreference.RadioGroupPreference(
                        title = context.getString(LR.string.settings_notification_play_over),
                        value = settings.playOverNotification.value,
                        preference = NotificationPreferences.SETTINGS_PLAY_OVER,
                        options = listOf(
                            PlayOverNotificationSetting.NEVER,
                            PlayOverNotificationSetting.DUCK,
                            PlayOverNotificationSetting.ALWAYS,
                        ),
                        displayText = context.getString(settings.playOverNotification.value.titleRes)
                    ),
                    NotificationPreference.SwitchPreference(
                        title = context.getString(LR.string.settings_notification_hide_on_pause),
                        value = settings.hideNotificationOnPause.value,
                        preference = NotificationPreferences.SETTINGS_HIDE_NOTIFICATION_ON_PAUSE
                    )
                )
            )
        )
    }

    private suspend fun getPodcastsSummary() = podcastManager.findSubscribedFlow().map { podcasts ->
        val podcastCount = podcasts.size
        val notificationCount = podcasts.count { it.isShowNotifications }

        when {
            notificationCount == 0 -> context.getString(LR.string.settings_podcasts_selected_zero)
            notificationCount == 1 -> context.getString(LR.string.settings_podcasts_selected_one)
            notificationCount >= podcastCount -> context.getString(LR.string.settings_podcasts_selected_all)
            else -> context.getString(
                LR.string.settings_podcasts_selected_x,
                notificationCount,
            )
        }
    }.take(1).firstOrNull()

    private fun getActionsSummary(): String {
        val userActions = settings.newEpisodeNotificationActions.value
        val actionStrings = userActions.joinToString { context.getString(it.labelId) }
        return if (userActions.isEmpty()) context.getString(LR.string.none) else actionStrings
    }

    private fun getNotificationSoundSummary() = settings.notificationSound.value.path.let { notificationSoundPath ->
        getRingtoneValue(notificationSoundPath)
    }

    private fun getRingtoneValue(ringtonePath: String?): String {
        if (ringtonePath.isNullOrBlank()) {
            return context.getString(LR.string.settings_notification_silent)
        }
        return when (val ringtone = RingtoneManager.getRingtone(context, ringtonePath.toUri())) {
            null -> ""
            else -> {
                val title = ringtone.getTitle(context)
                if (title == NotificationSound.defaultPath) {
                    context.getString(LR.string.settings_notification_default_sound)
                } else {
                    title
                }
            }
        }
    }

    override suspend fun setPreference(preference: NotificationPreference<*>) = withContext(dispatcher) {
        when (preference.preference) {
            NotificationPreferences.NEW_EPISODES_NOTIFY_ME -> {
                val value = (preference as NotificationPreference.SwitchPreference).value
                settings.notifyRefreshPodcast.set(value = value, updateModifiedAt = true)
                podcastManager.updateAllShowNotifications(value)
                if (value) {
                    settings.setNotificationLastSeenToNow()
                }
            }

            NotificationPreferences.SETTINGS_HIDE_NOTIFICATION_ON_PAUSE -> {
                settings.hideNotificationOnPause.set(value = (preference as NotificationPreference.SwitchPreference).value, updateModifiedAt = true)
            }

            NotificationPreferences.SETTINGS_PLAY_OVER -> {
                val setting = preference.value as? PlayOverNotificationSetting ?: return@withContext
                settings.playOverNotification.set(value = setting, updateModifiedAt = true)
            }

            NotificationPreferences.NEW_EPISODES_VIBRATION -> {
                val setting = (preference.value as? NotificationVibrateSetting) ?: NotificationVibrateSetting.DEFAULT
                settings.notificationVibrate.set(value = setting, updateModifiedAt = false)
            }

            NotificationPreferences.NEW_EPISODES_ACTIONS -> {
                val setting = preference as? NotificationPreference.MultiSelectPreference<*> ?: error("oopsie")
                settings.newEpisodeNotificationActions.set(value = setting.value.filterIsInstance<NewEpisodeNotificationAction>(), updateModifiedAt = true)
            }

            NotificationPreferences.NEW_EPISODES_RINGTONE -> {
                val setting = preference.value as? String ?: error("oopsie")
                settings.notificationSound.set(value = NotificationSound(setting, context), updateModifiedAt = false)
            }

            else -> Unit // TO BE IMPLEMENTED
        }
    }
}