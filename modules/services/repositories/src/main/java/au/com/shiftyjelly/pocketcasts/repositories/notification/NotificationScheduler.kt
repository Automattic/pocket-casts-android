package au.com.shiftyjelly.pocketcasts.repositories.notification

interface NotificationScheduler {
    fun setupOnboardingNotifications()
    suspend fun setupReEngagementNotification()
    suspend fun setupTrendingAndRecommendationsNotifications()
    suspend fun setupNewFeaturesAndTipsNotifications()
    suspend fun setupOffersNotifications()
    fun cancelScheduledReEngagementNotifications()
    fun cancelScheduledOnboardingNotifications()
    fun cancelScheduledTrendingAndRecommendationsNotifications()
    fun cancelScheduledNewFeaturesAndTipsNotifications()
    fun cancelScheduledOffersNotifications()
    fun cancelScheduledWorksByTag(list: List<String>)
}
