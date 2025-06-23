package au.com.shiftyjelly.pocketcasts.settings.notifications_testing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import au.com.shiftyjelly.pocketcasts.repositories.notification.NewFeaturesAndTipsNotificationType
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
internal class NotificationsTestingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<List<NotificationType>>(NotificationType.entries)
    val state: StateFlow<List<NotificationType>> = _state.asStateFlow()

    private val workManager by lazy { WorkManager.getInstance(context) }

    fun trigger(trigger: NotificationTrigger) {
        triggerOneTimeNotification(buildRequest(trigger))
    }

    private fun buildRequest(trigger: NotificationTrigger): OneTimeWorkRequest {
        val subCategory = when (trigger.notificationType) {
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

        return OneTimeWorkRequest.Builder(NotificationWorker::class.java)
            .setInputData(
                workDataOf(
                    SUBCATEGORY to subCategory,
                    SHOULD_SKIP_VALIDATIONS to true,
                    DOWNLOADED_EPISODES to 999
                )
            ).apply {
                if (trigger.triggerType is NotificationTriggerType.Delayed) {
                    setInitialDelay(trigger.triggerType.delaySeconds.toLong(), TimeUnit.SECONDS)
                }
            }
            .addTag(TAG_UNIQUE_WORK)
            .build()
    }

    private fun triggerOneTimeNotification(oneTimeWorkRequest: OneTimeWorkRequest) {
        workManager.enqueueUniqueWork(
            uniqueWorkName = TAG_UNIQUE_WORK,
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
            request = oneTimeWorkRequest
        )
    }

    sealed interface NotificationTriggerType {
        data object Now : NotificationTriggerType
        data class Delayed(val delaySeconds: Int) : NotificationTriggerType
    }

    data class NotificationTrigger(
        val notificationType: NotificationType,
        val triggerType: NotificationTriggerType
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
        OFFERS
    }

    companion object {
        const val TAG_UNIQUE_WORK = "unique_work_tag"
    }
}