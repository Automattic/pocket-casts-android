package au.com.shiftyjelly.pocketcasts.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.model.NewEpisodeNotificationAction
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationScheduler
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.notifications.data.NotificationsPreferenceRepository
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceCategory
import au.com.shiftyjelly.pocketcasts.settings.notifications.model.NotificationPreferenceType
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.NotificationsPermissionsOpenSystemSettingsEvent
import com.automattic.eventhorizon.SettingsDailyRemindersAdvancedSettingsTappedEvent
import com.automattic.eventhorizon.SettingsNotificationsActionsChangedEvent
import com.automattic.eventhorizon.SettingsNotificationsAdvancedSettingsTappedEvent
import com.automattic.eventhorizon.SettingsNotificationsDailyRemindersToggleEvent
import com.automattic.eventhorizon.SettingsNotificationsHidePlaybackNotificationOnPauseEvent
import com.automattic.eventhorizon.SettingsNotificationsNewEpisodesToggledEvent
import com.automattic.eventhorizon.SettingsNotificationsNewFeaturesAdvancedSettingsTappedEvent
import com.automattic.eventhorizon.SettingsNotificationsNewFeaturesToggleEvent
import com.automattic.eventhorizon.SettingsNotificationsOffersAdvancedSettingsTappedEvent
import com.automattic.eventhorizon.SettingsNotificationsOffersToggleEvent
import com.automattic.eventhorizon.SettingsNotificationsPlayOverNotificationsToggledEvent
import com.automattic.eventhorizon.SettingsNotificationsShownEvent
import com.automattic.eventhorizon.SettingsNotificationsSoundChangedEvent
import com.automattic.eventhorizon.SettingsNotificationsTrendingToggleEvent
import com.automattic.eventhorizon.SettingsNotificationsVibrationChangedEvent
import com.automattic.eventhorizon.SettingsTrendingAdvancedSettingsTappedEvent
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
    private val eventHorizon: EventHorizon,
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
                    eventHorizon.track(
                        SettingsNotificationsNewEpisodesToggledEvent(
                            enabled = preference.isEnabled,
                        ),
                    )
                }

                is NotificationPreferenceType.HidePlaybackNotificationOnPause -> {
                    preferenceRepository.setPreference(preference)
                    eventHorizon.track(
                        SettingsNotificationsHidePlaybackNotificationOnPauseEvent(
                            enabled = preference.isEnabled,
                        ),
                    )
                }

                is NotificationPreferenceType.PlayOverNotifications -> {
                    preferenceRepository.setPreference(preference)
                    eventHorizon.track(
                        SettingsNotificationsPlayOverNotificationsToggledEvent(
                            enabled = preference.value != PlayOverNotificationSetting.NEVER,
                            value = preference.value.analyticsValue,
                        ),
                    )
                }

                is NotificationPreferenceType.NotificationVibration -> {
                    preferenceRepository.setPreference(preference)
                    eventHorizon.track(
                        SettingsNotificationsVibrationChangedEvent(
                            value = preference.value.analyticsValue,
                        ),
                    )
                }

                is NotificationPreferenceType.AdvancedSettings -> {
                    eventHorizon.track(SettingsNotificationsAdvancedSettingsTappedEvent)
                }

                is NotificationPreferenceType.NotificationActions -> {
                    val previousValue = state.value.categories
                        .flatMap { it.preferences }
                        .find { it is NotificationPreferenceType.NotificationActions }
                    if ((previousValue as? NotificationPreferenceType.NotificationActions)?.value != preference.value) {
                        preferenceRepository.setPreference(preference)

                        eventHorizon.track(
                            SettingsNotificationsActionsChangedEvent(
                                actionArchive = NewEpisodeNotificationAction.Archive in preference.value,
                                actionDownload = NewEpisodeNotificationAction.Download in preference.value,
                                actionPlay = NewEpisodeNotificationAction.Play in preference.value,
                                actionPlayNext = NewEpisodeNotificationAction.PlayNext in preference.value,
                                actionPlayLast = NewEpisodeNotificationAction.PlayLast in preference.value,
                            ),
                        )
                    }
                }

                is NotificationPreferenceType.NotificationSoundPreference -> {
                    preferenceRepository.setPreference(preference)
                    eventHorizon.track(SettingsNotificationsSoundChangedEvent)
                }

                is NotificationPreferenceType.EnableDailyReminders -> {
                    preferenceRepository.setPreference(preference)
                    eventHorizon.track(
                        SettingsNotificationsDailyRemindersToggleEvent(
                            enabled = preference.isEnabled,
                        ),
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
                    eventHorizon.track(SettingsDailyRemindersAdvancedSettingsTappedEvent)
                }

                is NotificationPreferenceType.EnableRecommendations -> {
                    preferenceRepository.setPreference(preference)
                    eventHorizon.track(
                        SettingsNotificationsTrendingToggleEvent(
                            enabled = preference.isEnabled,
                        ),
                    )
                    if (preference.isEnabled) {
                        notificationScheduler.setupTrendingAndRecommendationsNotifications()
                    } else {
                        notificationScheduler.cancelScheduledTrendingAndRecommendationsNotifications()
                    }
                }

                is NotificationPreferenceType.RecommendationSettings -> {
                    eventHorizon.track(SettingsTrendingAdvancedSettingsTappedEvent)
                }

                is NotificationPreferenceType.EnableNewFeaturesAndTips -> {
                    preferenceRepository.setPreference(preference)
                    eventHorizon.track(
                        SettingsNotificationsNewFeaturesToggleEvent(
                            enabled = preference.isEnabled,
                        ),
                    )
                    if (preference.isEnabled) {
                        notificationScheduler.setupNewFeaturesAndTipsNotifications()
                    } else {
                        notificationScheduler.cancelScheduledNewFeaturesAndTipsNotifications()
                    }
                }

                is NotificationPreferenceType.NewFeaturesAndTipsSettings -> {
                    eventHorizon.track(SettingsNotificationsNewFeaturesAdvancedSettingsTappedEvent)
                }

                is NotificationPreferenceType.EnableOffers -> {
                    preferenceRepository.setPreference(preference)
                    eventHorizon.track(
                        SettingsNotificationsOffersToggleEvent(
                            enabled = preference.isEnabled,
                        ),
                    )
                    if (preference.isEnabled) {
                        notificationScheduler.setupOffersNotifications()
                    } else {
                        notificationScheduler.cancelScheduledOffersNotifications()
                    }
                }

                is NotificationPreferenceType.OffersSettings -> {
                    eventHorizon.track(SettingsNotificationsOffersAdvancedSettingsTappedEvent)
                }

                is NotificationPreferenceType.NotifyOnThesePodcasts -> Unit
            }
            loadPreferences()
        }
    }

    internal fun onShown() {
        eventHorizon.track(SettingsNotificationsShownEvent)
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
        eventHorizon.track(NotificationsPermissionsOpenSystemSettingsEvent)
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
