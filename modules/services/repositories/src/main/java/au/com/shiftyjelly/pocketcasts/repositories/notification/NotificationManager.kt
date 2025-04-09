package au.com.shiftyjelly.pocketcasts.repositories.notification

interface NotificationManager {
    suspend fun setupOnboardingNotificationsChannels()
    suspend fun trackFiltersInteractionFeature()
    suspend fun hasUserInteractedWithFiltersFeature(): Boolean
    suspend fun trackOnboardingNotificationSent(type: OnboardingNotificationType)
}
