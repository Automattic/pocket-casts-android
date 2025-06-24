package au.com.shiftyjelly.pocketcasts.repositories.notification

import kotlin.time.Duration

interface NotificationScheduler {
    fun setupOnboardingNotifications(delayProvider: ((OnboardingNotificationType) -> Duration)? = null)
    suspend fun setupReEngagementNotification(delayProvider: ((ReEngagementNotificationType) -> Duration)? = null)
    suspend fun setupTrendingAndRecommendationsNotifications(delayProvider: ((TrendingAndRecommendationsNotificationType) -> Duration)? = null)
    suspend fun setupNewFeaturesAndTipsNotifications(delayProvider: ((NewFeaturesAndTipsNotificationType) -> Duration)? = null)
    suspend fun setupOffersNotifications(delayProvider: ((OffersNotificationType) -> Duration)? = null)
    fun cancelScheduledReEngagementNotifications()
    fun cancelScheduledOnboardingNotifications()
    fun cancelScheduledTrendingAndRecommendationsNotifications()
    fun cancelScheduledNewFeaturesAndTipsNotifications()
    fun cancelScheduledOffersNotifications()
    fun cancelScheduledWorksByTag(tags: List<String>)
}
