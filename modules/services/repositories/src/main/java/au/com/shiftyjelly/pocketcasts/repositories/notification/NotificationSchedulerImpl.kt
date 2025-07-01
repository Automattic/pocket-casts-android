package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationWorker.Companion.DOWNLOADED_EPISODES
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationWorker.Companion.SUBCATEGORY
import au.com.shiftyjelly.pocketcasts.repositories.notification.ReEngagementNotificationType.Companion.SUBCATEGORY_REENGAGE_CATCH_UP_OFFLINE
import au.com.shiftyjelly.pocketcasts.repositories.notification.ReEngagementNotificationType.Companion.SUBCATEGORY_REENGAGE_WE_MISS_YOU
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.Util
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class NotificationSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val episodeManager: EpisodeManager,
    private val delayCalculator: NotificationDelayCalculator,
) : NotificationScheduler {

    companion object {
        const val TAG_TRENDING_RECOMMENDATIONS = "trending_and_recommendations"
        private const val TAG_REENGAGEMENT = "daily_re_engagement_check"
        private const val TAG_ONBOARDING = "onboarding_notification"
        private const val TAG_FEATURES = "features_and_tips"
        private const val TAG_OFFERS = "offers"
    }

    override fun setupOnboardingNotifications(delayProvider: ((OnboardingNotificationType) -> Duration)?) {
        if (!isRunningOnPhone) return

        listOf(
            OnboardingNotificationType.Sync,
            OnboardingNotificationType.Import,
            OnboardingNotificationType.UpNext,
            OnboardingNotificationType.Filters,
            OnboardingNotificationType.Themes,
            OnboardingNotificationType.StaffPicks,
            OnboardingNotificationType.PlusUpsell,
        ).forEach { type ->
            val delay = delayProvider?.invoke(type)?.inWholeMilliseconds ?: delayCalculator.calculateDelayForOnboardingNotification(type)

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

    override suspend fun setupReEngagementNotification(delayProvider: ((ReEngagementNotificationType) -> Duration)?) {
        if (!isRunningOnPhone) return

        val initialDelay = delayProvider?.invoke(ReEngagementNotificationType.WeMissYou)?.inWholeMilliseconds ?: delayCalculator.calculateDelayForReEngagementCheck()

        val downloadedEpisodes = episodeManager.downloadedEpisodesThatHaveNotBeenPlayedCount()
        val subcategory =
            if (downloadedEpisodes > 0) SUBCATEGORY_REENGAGE_CATCH_UP_OFFLINE else SUBCATEGORY_REENGAGE_WE_MISS_YOU

        val workData = workDataOf(
            SUBCATEGORY to subcategory,
            DOWNLOADED_EPISODES to downloadedEpisodes,
        )

        val notificationWork = PeriodicWorkRequest.Builder(NotificationWorker::class.java, 7, TimeUnit.DAYS)
            .setInputData(workData)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(TAG_REENGAGEMENT)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TAG_REENGAGEMENT,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            notificationWork,
        )
    }

    override suspend fun setupTrendingAndRecommendationsNotifications(delayProvider: ((TrendingAndRecommendationsNotificationType) -> Duration)?) {
        if (!isRunningOnPhone) return

        TrendingAndRecommendationsNotificationType.values.forEachIndexed { index, notification ->
            val initialDelay = delayProvider?.invoke(notification)?.inWholeMilliseconds ?: delayCalculator.calculateDelayForRecommendations(index)
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

    override suspend fun setupNewFeaturesAndTipsNotifications(delayProvider: ((NewFeaturesAndTipsNotificationType) -> Duration)?) {
        if (!isRunningOnPhone) return

        // this should be later updated to fire the desired feature for the given release
        val workData = workDataOf(
            SUBCATEGORY to NewFeaturesAndTipsNotificationType.SmartFolders.subcategory,
        )
        val notificationWork = OneTimeWorkRequest.Builder(NotificationWorker::class.java)
            .setInputData(workData)
            .setInitialDelay(delayProvider?.invoke(NewFeaturesAndTipsNotificationType.SmartFolders)?.inWholeMilliseconds ?: delayCalculator.calculateDelayForNewFeatures(), TimeUnit.MILLISECONDS)
            .addTag(TAG_FEATURES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            TAG_FEATURES,
            ExistingWorkPolicy.KEEP,
            notificationWork,
        )
    }

    override suspend fun setupOffersNotifications(delayProvider: ((OffersNotificationType) -> Duration)?) {
        if (!isRunningOnPhone) return

        val workData = workDataOf(
            SUBCATEGORY to OffersNotificationType.UpgradeNow.subcategory,
        )
        val notificationWork = PeriodicWorkRequest.Builder(NotificationWorker::class.java, 14, TimeUnit.DAYS)
            .setInputData(workData)
            .setInitialDelay(delayProvider?.invoke(OffersNotificationType.UpgradeNow)?.inWholeMilliseconds ?: delayCalculator.calculateDelayForOffers(), TimeUnit.MILLISECONDS)
            .addTag(TAG_OFFERS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TAG_OFFERS,
            ExistingPeriodicWorkPolicy.UPDATE,
            notificationWork,
        )
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

    override fun cancelScheduledNewFeaturesAndTipsNotifications() {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_FEATURES)
    }

    override fun cancelScheduledOffersNotifications() {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_OFFERS)
    }

    override fun cancelScheduledWorksByTag(tags: List<String>) {
        with(WorkManager.getInstance(context)) {
            tags.forEach {
                cancelAllWorkByTag(it)
            }
        }
    }

    private val isRunningOnPhone = Util.getAppPlatform(context) == AppPlatform.Phone
}
