package au.com.shiftyjelly.pocketcasts.repositories.notification

import au.com.shiftyjelly.pocketcasts.models.db.dao.UserNotificationsDao
import au.com.shiftyjelly.pocketcasts.models.entity.UserNotifications
import javax.inject.Inject

class NotificationManagerImpl @Inject constructor(
    private val userNotificationsDao: UserNotificationsDao,
) : NotificationManager {

    override suspend fun setupOnboardingNotifications() {
        val userNotifications = OnboardingNotificationType.values.map { notification ->
            UserNotifications(notificationId = notification.notificationId)
        }

        userNotificationsDao.insert(userNotifications)
    }

    override suspend fun trackUserInteractedWithFeature(type: OnboardingNotificationType) {
        userNotificationsDao.updateInteractedAt(type.notificationId, System.currentTimeMillis())
    }

    override suspend fun hasUserInteractedWithFeature(type: OnboardingNotificationType): Boolean {
        val userNotification = userNotificationsDao.getUserNotification(type.notificationId)
            ?: return false

        return userNotification.interactedAt != null
    }

    override suspend fun updateOnboardingNotificationSent(type: OnboardingNotificationType) {
        userNotificationsDao.getUserNotification(type.notificationId)
            ?.apply {
                sentThisWeek++
                lastSentAt = System.currentTimeMillis()
            }
            ?.let { userNotificationsDao.update(it) }
    }
}
