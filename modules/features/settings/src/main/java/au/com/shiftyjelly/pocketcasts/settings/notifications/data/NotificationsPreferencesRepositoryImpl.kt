package au.com.shiftyjelly.pocketcasts.settings.notifications.data

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import au.com.shiftyjelly.pocketcasts.preferences.NotificationSound
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.LocalPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreference
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceCategory
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
                        NotificationPreference.ValuePreference.SwitchPreference(
                            title = context.getString(LR.string.settings_notification_notify_me),
                            value = isEnabled,
                            preferenceKey = PREF_NOTIFY_ME
                        )
                    )
                    if (isEnabled) {
                        add(
                            NotificationPreference.ValuePreference.TextPreference.MultiSelectPreference(
                                title = context.getString(LR.string.settings_notification_choose_podcasts),
                                value = getPodcastsSummary().orEmpty(),
                                preferenceKey = ""
                            )
                        )
                        add(
                            NotificationPreference.ValuePreference.TextPreference.MultiSelectPreference(
                                title = context.getString(LR.string.settings_notification_actions_title),
                                value = getActionsSummary(),
                                preferenceKey = ""
                            )
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            add(
                                NotificationPreference.ExternalPreference(
                                    title = context.getString(LR.string.settings_notification_advanced),
                                    description = context.getString(LR.string.settings_notification_advanced_summary)
                                )
                            )
                        } else {
                            add(
                                NotificationPreference.ExternalPreference(
                                    title = context.getString(LR.string.settings_notification_sound),
                                    description = getNotificationSoundSummary()
                                )
                            )
                            add(
                                NotificationPreference.ExternalPreference(
                                    title = context.getString(LR.string.settings_notification_vibrate),
                                    description = context.getString(settings.notificationVibrate.value.summary)
                                )
                            )
                        }
                    }
                }
            ),
            NotificationPreferenceCategory(
                title = context.getString(LR.string.settings),
                preferences = listOf(
                    NotificationPreference.ValuePreference.TextPreference.SingleSelectPreference(
                        title = context.getString(LR.string.settings_notification_play_over),
                        value = context.getString(settings.playOverNotification.value.titleRes),
                        preferenceKey = ""
                    ),
                    NotificationPreference.ValuePreference.SwitchPreference(
                        title = context.getString(LR.string.settings_notification_hide_on_pause),
                        value = settings.hideNotificationOnPause.value,
                        preferenceKey = ""
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
        // TODO do we need to store it?
        // it.setDefaultValue(notificationSoundPath)
        getRingtoneValue(notificationSoundPath)
    }

    private fun getRingtoneValue(ringtonePath: String?): String {
        if (ringtonePath.isNullOrBlank()) {
            return context.getString(LR.string.settings_notification_silent)
        }
        return when (val ringtone = RingtoneManager.getRingtone(context, Uri.parse(ringtonePath))) {
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

    override suspend fun setPreference(preference: LocalPreference) = withContext(dispatcher) {
        // TO be implemented later
    }

    private companion object {
        const val PREF_NOTIFY_ME = "notifyMe"
    }
}