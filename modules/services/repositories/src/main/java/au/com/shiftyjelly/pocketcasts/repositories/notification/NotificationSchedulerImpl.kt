package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.content.Context
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import java.util.concurrent.TimeUnit

class NotificationSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationScheduler {

    override fun setupOnboardingNotifications() {
        val delayCalculator = NotificationDelayCalculator()

        listOf(
            OnboardingNotificationType.Sync,
            OnboardingNotificationType.Import,
            OnboardingNotificationType.UpNext,
            OnboardingNotificationType.Filters,
            OnboardingNotificationType.Themes,
            OnboardingNotificationType.StaffPicks,
            OnboardingNotificationType.PlusUpsell,
        ).forEach { type ->
            val delay = delayCalculator.calculateDelayForOnboardingNotification(type)

            val workData = workDataOf(
                "subcategory" to type.subcategory,
            )

            val notificationWork = OneTimeWorkRequest.Builder(NotificationWorker::class.java)
                .setInputData(workData)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("onboarding_notification_${type.subcategory}")
                .build()

            WorkManager.getInstance(context).enqueue(notificationWork)
        }
    }
}
