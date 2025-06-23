package au.com.shiftyjelly.pocketcasts.settings.notifications_testing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationType
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationWorker
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationWorker.Companion.SHOULD_SKIP_VALIDATIONS
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationWorker.Companion.SUBCATEGORY
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
        val workData = when (trigger.notificationType) {
            NotificationType.TRENDING -> {
                workDataOf(
                    SUBCATEGORY to TrendingAndRecommendationsNotificationType.SUBCATEGORY_TRENDING,
                    SHOULD_SKIP_VALIDATIONS to true
                )
            }
            NotificationType.RECOMMENDATIONS -> {
                workDataOf(
                    SUBCATEGORY to TrendingAndRecommendationsNotificationType.SUBCATEGORY_RECOMMENDATIONS,
                    SHOULD_SKIP_VALIDATIONS to true
                )
            }
            NotificationType.DAILY_REMINDER_X -> TODO()
            NotificationType.NEW_FEATURE_X -> TODO()
            NotificationType.TIPS -> TODO()
            NotificationType.OFFERS -> TODO()
        }

        return OneTimeWorkRequest.Builder(NotificationWorker::class.java)
            .setInputData(workData).apply {
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
        DAILY_REMINDER_X,
        NEW_FEATURE_X,
        TIPS,
        OFFERS
    }

    companion object {
        const val TAG_UNIQUE_WORK = "unique_work_tag"
    }
}