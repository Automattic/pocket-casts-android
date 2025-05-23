package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import au.com.shiftyjelly.pocketcasts.repositories.notification.ReEngagementNotificationType.Companion.SUBCATEGORY_REENGAGE_CATCH_UP_OFFLINE
import au.com.shiftyjelly.pocketcasts.repositories.notification.ReEngagementNotificationType.Companion.SUBCATEGORY_REENGAGE_WE_MISS_YOU
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import java.util.concurrent.TimeUnit

class NotificationSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val episodeManager: EpisodeManager,
    private val delayCalculator: NotificationDelayCalculator,
) : NotificationScheduler {

    companion object {
        const val SUBCATEGORY = "subcategory"
        const val DOWNLOADED_EPISODES = "downloaded_episodes"

        private const val TAG_TRENDING_RECOMMENDATIONS = "trending_and_recommendations"
        private const val TAG_REENGAGEMENT = "daily_re_engagement_check"
        private const val TAG_ONBOARDING = "onboarding_notification"
    }

    override fun setupOnboardingNotifications() {
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
                SUBCATEGORY to type.subcategory,
            )

            val notificationWork = OneTimeWorkRequest.Builder(NotificationWorker::class.java)
                .setInputData(workData)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("${TAG_ONBOARDING}_${type.subcategory}")
                .build()

            WorkManager.getInstance(context).enqueue(notificationWork)
        }
    }

    override suspend fun setupReEngagementNotification() {
        val initialDelay = delayCalculator.calculateDelayForReEngagementCheck()

        val downloadedEpisodes = episodeManager.downloadedEpisodesThatHaveNotBeenPlayedCount()
        val subcategory =
            if (downloadedEpisodes > 0) SUBCATEGORY_REENGAGE_CATCH_UP_OFFLINE else SUBCATEGORY_REENGAGE_WE_MISS_YOU

        val workData = workDataOf(
            SUBCATEGORY to subcategory,
            DOWNLOADED_EPISODES to downloadedEpisodes,
        )

        val notificationWork = PeriodicWorkRequest.Builder(NotificationWorker::class.java, 1, TimeUnit.DAYS)
            .setInputData(workData)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(TAG_REENGAGEMENT)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TAG_REENGAGEMENT,
            ExistingPeriodicWorkPolicy.UPDATE,
            notificationWork,
        )
    }

    override suspend fun setupTrendingAndRecommendationsNotifications() {
        TrendingAndRecommendationsNotificationType.values.forEachIndexed { index, notification ->
            val initialDelay = delayCalculator.calculateDelayForRecommendations(index)
            val workData = workDataOf(
                SUBCATEGORY to notification.subcategory,
            )

            val tag = "$TAG_TRENDING_RECOMMENDATIONS-${notification.subcategory}"
            val notificationWork = PeriodicWorkRequest.Builder(NotificationWorker::class.java, 7, TimeUnit.DAYS)
                .setInputData(workData)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(tag)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                tag,
                ExistingPeriodicWorkPolicy.UPDATE,
                notificationWork,
            )
        }
    }

    override fun cancelScheduledReEngagementNotifications() {
        WorkManager.getInstance(context).cancelUniqueWork(TAG_REENGAGEMENT)
    }

    override fun cancelScheduledOnboardingNotifications() {
        OnboardingNotificationType.values.forEach {
            WorkManager.getInstance(context).cancelAllWorkByTag("${TAG_ONBOARDING}_${it.subcategory}")
        }
    }

    override fun cancelScheduledTrendingAndRecommendationsNotifications() {
        TrendingAndRecommendationsNotificationType.values.forEach {
            WorkManager.getInstance(context).cancelUniqueWork("$TAG_TRENDING_RECOMMENDATIONS-${it.subcategory}")
        }
    }
}
