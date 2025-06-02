package au.com.shiftyjelly.pocketcasts.repositories.notification

interface NotificationManager {
    suspend fun setupOnboardingNotifications()
    suspend fun setupReEngagementNotifications()
    suspend fun setupTrendingAndRecommendationsNotifications()
    suspend fun setupNewFeaturesNotifications()
    suspend fun setupOffersNotifications()
    suspend fun updateUserFeatureInteraction(type: NotificationType)
    suspend fun updateUserFeatureInteraction(id: Int)
    suspend fun hasUserInteractedWithFeature(type: NotificationType): Boolean
    suspend fun updateNotificationSent(type: NotificationType)
}
