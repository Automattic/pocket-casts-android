package au.com.shiftyjelly.pocketcasts.repositories.notification

interface NotificationManager {
    suspend fun setupOnboardingNotifications()
    suspend fun setupReEngagementNotifications()
    suspend fun updateUserFeatureInteraction(type: NotificationType)
    suspend fun hasUserInteractedWithFeature(type: NotificationType): Boolean
    suspend fun updateOnboardingNotificationSent(type: NotificationType)
}
