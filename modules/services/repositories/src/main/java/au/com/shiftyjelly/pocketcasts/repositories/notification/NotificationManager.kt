package au.com.shiftyjelly.pocketcasts.repositories.notification

interface NotificationManager {
    suspend fun setupOnboardingNotificationsChannels()
    suspend fun trackUserInteractedWithFeature(type: OnboardingNotificationType)
    suspend fun hasUserInteractedWithFeature(type: OnboardingNotificationType): Boolean
    suspend fun trackOnboardingNotificationSent(type: OnboardingNotificationType)
}
