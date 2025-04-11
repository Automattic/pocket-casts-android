package au.com.shiftyjelly.pocketcasts.repositories.notification

interface NotificationManager {
    suspend fun setupOnboardingNotifications()
    suspend fun trackUserInteractedWithFeature(type: OnboardingNotificationType)
    suspend fun hasUserInteractedWithFeature(type: OnboardingNotificationType): Boolean
    suspend fun updateOnboardingNotificationSent(type: OnboardingNotificationType)
}
