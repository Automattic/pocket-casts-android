package au.com.shiftyjelly.pocketcasts.settings.notificationstesting

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserNotificationsDao
import au.com.shiftyjelly.pocketcasts.repositories.notification.NewFeaturesAndTipsNotificationType
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationScheduler
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationWorker
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationWorker.Companion.DOWNLOADED_EPISODES
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationWorker.Companion.SHOULD_SKIP_VALIDATIONS
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationWorker.Companion.SUBCATEGORY
import au.com.shiftyjelly.pocketcasts.repositories.notification.OffersNotificationType
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType
import au.com.shiftyjelly.pocketcasts.repositories.notification.ReEngagementNotificationType
import au.com.shiftyjelly.pocketcasts.repositories.notification.TrendingAndRecommendationsNotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
internal class NotificationsTestingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationScheduler: NotificationScheduler,
    private val userNotificationsDao: UserNotificationsDao,
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val workManager by lazy { WorkManager.getInstance(context) }

    fun trigger(trigger: NotificationTrigger) {
        val subCategory = trigger.notificationType.subCategory
        triggerOneTimeNotification(buildRequest(trigger), "$TAG_UNIQUE_WORK-$subCategory")
    }

    private val NotificationType.subCategory get() = when (this) {
        NotificationType.TRENDING -> TrendingAndRecommendationsNotificationType.Trending.subcategory
        NotificationType.RECOMMENDATIONS -> TrendingAndRecommendationsNotificationType.Recommendations.subcategory
        NotificationType.NEW_FEATURE_FOLDERS -> NewFeaturesAndTipsNotificationType.SmartFolders.subcategory
        NotificationType.OFFERS -> OffersNotificationType.UpgradeNow.subcategory
        NotificationType.DAILY_REMINDER_MISS_YOU -> ReEngagementNotificationType.WeMissYou.subcategory
        NotificationType.DAILY_REMINDER_DOWNLOADS_OFFLINE -> ReEngagementNotificationType.CatchUpOffline.subcategory
        NotificationType.DAILY_REMINDER_SYNC -> OnboardingNotificationType.Sync.subcategory
        NotificationType.DAILY_REMINDER_IMPORT -> OnboardingNotificationType.Import.subcategory
        NotificationType.DAILY_REMINDER_UP_NEXT -> OnboardingNotificationType.UpNext.subcategory
        NotificationType.DAILY_REMINDER_FILTERS -> OnboardingNotificationType.Filters.subcategory
        NotificationType.DAILY_REMINDERS_STAFF_PICKS -> OnboardingNotificationType.StaffPicks.subcategory
        NotificationType.DAILY_REMINDERS_THEMES -> OnboardingNotificationType.Themes.subcategory
        NotificationType.DAILY_REMINDERS_UPSELL -> OnboardingNotificationType.PlusUpsell.subcategory
    }

    private fun buildRequest(trigger: NotificationTrigger): OneTimeWorkRequest {
        val subCategory = trigger.notificationType.subCategory
        return OneTimeWorkRequest.Builder(NotificationWorker::class.java)
            .setInputData(
                workDataOf(
                    SUBCATEGORY to subCategory,
                    SHOULD_SKIP_VALIDATIONS to true,
                    DOWNLOADED_EPISODES to 999,
                ),
            ).apply {
                if (trigger.triggerType is NotificationTriggerType.Delayed) {
                    setInitialDelay(trigger.triggerType.delay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                }
            }
            .addTag("$TAG_UNIQUE_WORK-$subCategory")
            .build()
    }

    private fun triggerOneTimeNotification(oneTimeWorkRequest: OneTimeWorkRequest, tag: String) {
        workManager.enqueueUniqueWork(
            uniqueWorkName = tag,
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
            request = oneTimeWorkRequest,
        )
    }

    fun clearNotificationsTable() {
        viewModelScope.launch {
            userNotificationsDao.deleteAll()
        }
    }

    fun cancelAllNotifications() {
        viewModelScope.launch {
            notificationScheduler.cancelScheduledReEngagementNotifications()
            notificationScheduler.cancelScheduledOffersNotifications()
            notificationScheduler.cancelScheduledOnboardingNotifications()
            notificationScheduler.cancelScheduledTrendingAndRecommendationsNotifications()
            notificationScheduler.cancelScheduledNewFeaturesAndTipsNotifications()
            notificationScheduler.cancelScheduledOffersNotifications()
        }
    }

    fun scheduleCategory(schedule: NotificationCategorySchedule) {
        viewModelScope.launch {
            when (schedule.category) {
                NotificationCategoryType.DAILY_REMINDERS -> {
                    notificationScheduler.setupOnboardingNotifications {
                        val indexOfType = OnboardingNotificationType.values.indexOf(it)
                        schedule.consecutiveDelay * (1 + indexOfType)
                    }
                    notificationScheduler.setupReEngagementNotification {
                        val indexOfType = ReEngagementNotificationType.values.indexOf(it)
                        schedule.consecutiveDelay * (1 + indexOfType)
                    }
                }

                NotificationCategoryType.TRENDING_AND_RECOMMENDATIONS -> notificationScheduler.setupTrendingAndRecommendationsNotifications {
                    val indexOfType = TrendingAndRecommendationsNotificationType.values.indexOf(it)
                    schedule.consecutiveDelay * (1 + indexOfType)
                }

                NotificationCategoryType.NEW_FEATURES_AND_TIPS -> notificationScheduler.setupNewFeaturesAndTipsNotifications {
                    val indexOfType = NewFeaturesAndTipsNotificationType.values.indexOf(it)
                    schedule.consecutiveDelay * (1 + indexOfType)
                }

                NotificationCategoryType.POCKET_CASTS_OFFERS -> notificationScheduler.setupOffersNotifications {
                    val indexOfType = OffersNotificationType.values.indexOf(it)
                    schedule.consecutiveDelay * (1 + indexOfType)
                }
            }
        }
    }

    data class UiState(
        val uniqueNotifications: List<NotificationType> = NotificationType.entries,
        val notificationCategories: List<NotificationCategoryType> = NotificationCategoryType.entries,
    )

    sealed interface NotificationTriggerType {
        data object Now : NotificationTriggerType
        data class Delayed(val delay: Duration) : NotificationTriggerType
    }

    data class NotificationTrigger(
        val notificationType: NotificationType,
        val triggerType: NotificationTriggerType,
    )

    data class NotificationCategorySchedule(
        val category: NotificationCategoryType,
        val consecutiveDelay: Duration,
    )

    enum class NotificationType {
        TRENDING,
        RECOMMENDATIONS,
        DAILY_REMINDER_MISS_YOU,
        DAILY_REMINDER_DOWNLOADS_OFFLINE,
        DAILY_REMINDER_SYNC,
        DAILY_REMINDER_IMPORT,
        DAILY_REMINDER_UP_NEXT,
        DAILY_REMINDER_FILTERS,
        DAILY_REMINDERS_STAFF_PICKS,
        DAILY_REMINDERS_THEMES,
        DAILY_REMINDERS_UPSELL,
        NEW_FEATURE_FOLDERS,
        OFFERS,
    }

    enum class NotificationCategoryType {
        TRENDING_AND_RECOMMENDATIONS,
        DAILY_REMINDERS,
        NEW_FEATURES_AND_TIPS,
        POCKET_CASTS_OFFERS,
    }

    companion object {
        const val TAG_UNIQUE_WORK = "unique_work_tag"
    }
}
