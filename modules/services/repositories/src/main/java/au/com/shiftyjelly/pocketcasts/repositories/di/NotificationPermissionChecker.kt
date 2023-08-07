package au.com.shiftyjelly.pocketcasts.repositories.di

interface NotificationPermissionChecker {
    fun checkNotificationPermission(onPermissionGranted: () -> Unit)
}
