package au.com.shiftyjelly.pocketcasts.repositories.notification

interface NotificationManager {
    suspend fun setupOnboardingNotifications()
    suspend fun trackFiltersInteractionFeature()
    suspend fun hasUserInteractedWithFiltersFeature(): Boolean
}
