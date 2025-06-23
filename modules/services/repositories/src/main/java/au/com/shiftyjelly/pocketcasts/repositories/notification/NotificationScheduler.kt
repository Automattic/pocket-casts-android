package au.com.shiftyjelly.pocketcasts.repositories.notification

interface NotificationScheduler {
    fun setupOnboardingNotifications(delayProvider: ((OnboardingNotificationType) -> Long)? = null)
    suspend fun setupReEngagementNotification(delayProvider: ((ReEngagementNotificationType) -> Long)? = null)
    suspend fun setupTrendingAndRecommendationsNotifications(delayProvider: ((TrendingAndRecommendationsNotificationType) -> Long)? = null)
    suspend fun setupNewFeaturesAndTipsNotifications(delayProvider: ((NewFeaturesAndTipsNotificationType) -> Long)? = null)
    suspend fun setupOffersNotifications(delayProvider: ((OffersNotificationType) -> Long)? = null)
    fun cancelScheduledReEngagementNotifications()
    fun cancelScheduledOnboardingNotifications()
    fun cancelScheduledTrendingAndRecommendationsNotifications()
    fun cancelScheduledNewFeaturesAndTipsNotifications()
    fun cancelScheduledOffersNotifications()
    fun cancelScheduledWorksByTag(tags: List<String>)
}
