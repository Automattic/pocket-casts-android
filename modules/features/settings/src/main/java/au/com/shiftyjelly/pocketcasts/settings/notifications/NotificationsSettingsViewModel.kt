package au.com.shiftyjelly.pocketcasts.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationAction
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationScheduler
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.notifications.data.NotificationsPreferenceRepository
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceCategory
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
internal class NotificationsSettingsViewModel @Inject constructor(
    private val preferenceRepository: NotificationsPreferenceRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val podcastManager: PodcastManager,
    private val notificationHelper: NotificationHelper,
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(
        State(
            areSystemNotificationsEnabled = notificationHelper.hasNotificationsPermission(),
            categories = emptyList(),
        ),
    )
    val state: StateFlow<State> = _state

    init {
        loadPreferences()
    }

    internal fun loadPreferences() = viewModelScope.launch {
        _state.update { it.copy(categories = preferenceRepository.getPreferenceCategories()) }
    }

    internal fun onPreferenceChanged(preference: NotificationPreferenceType) {
        viewModelScope.launch {
            when (preference) {
                is NotificationPreferenceType.NotifyMeOnNewEpisodes -> {
                    preferenceRepository.setPreference(preference)
                    analyticsTracker.track(
                        AnalyticsEvent.SETTINGS_NOTIFICATIONS_NEW_EPISODES_TOGGLED,
                        mapOf("enabled" to preference.isEnabled),
                    )
                }

                is NotificationPreferenceType.HidePlaybackNotificationOnPause -> {
                    preferenceRepository.setPreference(preference)
                    analyticsTracker.track(
                        AnalyticsEvent.SETTINGS_NOTIFICATIONS_HIDE_PLAYBACK_NOTIFICATION_ON_PAUSE,
                        mapOf("enabled" to preference.isEnabled),
                    )
                }

                is NotificationPreferenceType.PlayOverNotifications -> {
                    preferenceRepository.setPreference(preference)
                    analyticsTracker.track(
                        AnalyticsEvent.SETTINGS_NOTIFICATIONS_PLAY_OVER_NOTIFICATIONS_TOGGLED,
                        mapOf(
                            "enabled" to (preference.value != PlayOverNotificationSetting.NEVER),
                            "value" to preference.value.analyticsString,
                        ),
                    )
                }

                is NotificationPreferenceType.NotificationVibration -> {
                    preferenceRepository.setPreference(preference)
                    analyticsTracker.track(
                        AnalyticsEvent.SETTINGS_NOTIFICATIONS_VIBRATION_CHANGED,
                        mapOf("value" to preference.value.analyticsString),
                    )
                }

                is NotificationPreferenceType.AdvancedSettings -> {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_ADVANCED_SETTINGS_TAPPED)
                }

                is NotificationPreferenceType.NotificationActions -> {
                    val previousValue = state.value.categories.map { it.preferences }.flatten()
                        .find { it is NotificationPreferenceType.NotificationActions }
                    if ((previousValue as? NotificationPreferenceType.NotificationActions)?.value != preference.value) {
                        preferenceRepository.setPreference(preference)

                        analyticsTracker.track(
                            AnalyticsEvent.SETTINGS_NOTIFICATIONS_ACTIONS_CHANGED,
                            mapOf(
                                "action_archive" to preference.value.contains(NewEpisodeNotificationAction.Archive),
                                "action_download" to preference.value.contains(NewEpisodeNotificationAction.Download),
                                "action_play" to preference.value.contains(NewEpisodeNotificationAction.Play),
                                "action_play_next" to preference.value.contains(NewEpisodeNotificationAction.PlayNext),
                                "action_play_last" to preference.value.contains(NewEpisodeNotificationAction.PlayLast),
                            ),
                        )
                    }
                }

                is NotificationPreferenceType.NotificationSoundPreference -> {
                    preferenceRepository.setPreference(preference)
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_SOUND_CHANGED)
                }

                is NotificationPreferenceType.EnableDailyReminders -> {
                    preferenceRepository.setPreference(preference)
                    analyticsTracker.track(
                        AnalyticsEvent.SETTINGS_NOTIFICATIONS_DAILY_REMINDERS_TOGGLED,
                        mapOf("enabled" to preference.isEnabled),
                    )
                    if (preference.isEnabled) {
                        notificationScheduler.setupOnboardingNotifications()
                        notificationScheduler.setupReEngagementNotification()
                    } else {
                        notificationScheduler.cancelScheduledOnboardingNotifications()
                        notificationScheduler.cancelScheduledReEngagementNotifications()
                    }
                }

                is NotificationPreferenceType.DailyReminderSettings -> {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_DAILY_REMINDERS_ADVANCED_SETTINGS_TAPPED)
                }

                is NotificationPreferenceType.EnableRecommendations -> {
                    preferenceRepository.setPreference(preference)
                    analyticsTracker.track(
                        AnalyticsEvent.SETTINGS_NOTIFICATIONS_TRENDING_AND_RECOMMENDATIONS_TOGGLED,
                        mapOf("enabled" to preference.isEnabled),
                    )
                    if (preference.isEnabled) {
                        notificationScheduler.setupTrendingAndRecommendationsNotifications()
                    } else {
                        notificationScheduler.cancelScheduledTrendingAndRecommendationsNotifications()
                    }
                }

                is NotificationPreferenceType.RecommendationSettings -> {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_TRENDING_AND_RECOMMENDATIONS_ADVANCED_SETTINGS_TAPPED)
                }

                is NotificationPreferenceType.EnableNewFeaturesAndTips -> {
                    preferenceRepository.setPreference(preference)
                    analyticsTracker.track(
                        AnalyticsEvent.SETTINGS_NOTIFICATIONS_NEW_FEATURES_AND_TIPS_TOGGLED,
                        mapOf("enabled" to preference.isEnabled),
                    )
                    if (preference.isEnabled) {
                        notificationScheduler.setupNewFeaturesAndTipsNotifications()
                    } else {
                        notificationScheduler.cancelScheduledNewFeaturesAndTipsNotifications()
                    }
                }

                is NotificationPreferenceType.NewFeaturesAndTipsSettings -> {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_NEW_FEATURES_AND_TIPS_ADVANCED_SETTINGS_TAPPED)
                }

                is NotificationPreferenceType.EnableOffers -> {
                    preferenceRepository.setPreference(preference)
                    analyticsTracker.track(
                        AnalyticsEvent.SETTINGS_NOTIFICATIONS_OFFERS_TOGGLED,
                        mapOf("enabled" to preference.isEnabled),
                    )
                    if (preference.isEnabled) {
                        notificationScheduler.setupOffersNotifications()
                    } else {
                        notificationScheduler.cancelScheduledOffersNotifications()
                    }
                }

                is NotificationPreferenceType.OffersSettings -> {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_OFFERS_ADVANCED_SETTINGS_TAPPED)
                }

                is NotificationPreferenceType.NotifyOnThesePodcasts -> Unit
            }
            loadPreferences()
        }
    }

    internal fun onShown() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_SHOWN)
    }

    internal fun checkNotificationPermission() {
        _state.update {
            it.copy(areSystemNotificationsEnabled = notificationHelper.hasNotificationsPermission())
        }
    }

    internal fun onSelectedPodcastsChanged(newSelection: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            podcastManager.findSubscribedBlocking().forEach {
                podcastManager.updateShowNotifications(it.uuid, newSelection.contains(it.uuid))
            }
            loadPreferences()
        }
    }

    internal fun reportSystemNotificationsSettingsOpened() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_PERMISSION_OPEN_SYSTEM_SETTINGS)
    }

    internal suspend fun getSelectedPodcastIds(): List<String> = withContext(Dispatchers.IO) {
        val uuids = podcastManager.findSubscribedBlocking().filter { it.isShowNotifications }.map { it.uuid }
        uuids
    }

    internal data class State(
        val areSystemNotificationsEnabled: Boolean,
        val categories: List<NotificationPreferenceCategory>,
    )
}
