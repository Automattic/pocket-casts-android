package au.com.shiftyjelly.pocketcasts.repositories.notification

interface NotificationScheduler {
    fun setupOnboardingNotifications()
    suspend fun setupReEngagementNotification()
}
