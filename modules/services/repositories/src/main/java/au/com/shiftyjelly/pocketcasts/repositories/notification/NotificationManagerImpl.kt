package au.com.shiftyjelly.pocketcasts.repositories.notification

import au.com.shiftyjelly.pocketcasts.models.db.dao.UserNotificationsDao
import au.com.shiftyjelly.pocketcasts.models.entity.UserNotifications
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class NotificationManagerImpl @Inject constructor(
    private val userNotificationsDao: UserNotificationsDao,
) : NotificationManager {

    override suspend fun setupOnboardingNotifications() {
        setupNotificationsForType(OnboardingNotificationType.values) { it.notificationId }
    }

    override suspend fun setupReEngagementNotifications() {
        setupNotificationsForType(ReEngagementNotificationType.values) { it.notificationId }
    }

    override suspend fun updateUserFeatureInteraction(type: NotificationType) {
        userNotificationsDao.updateInteractedAt(type.notificationId, System.currentTimeMillis())
    }

    override suspend fun updateUserFeatureInteraction(id: Int) {
        userNotificationsDao.updateInteractedAt(id, System.currentTimeMillis())
    }

    override suspend fun hasUserInteractedWithFeature(type: NotificationType): Boolean {
        val userNotification = userNotificationsDao.getUserNotification(type.notificationId)
            ?: return false

        if (type is ReEngagementNotificationType) {
            val lastInteraction = userNotification.interactedAt ?: return false
            val elapsed = (System.currentTimeMillis() - lastInteraction).milliseconds
            return elapsed < 7.days
        }

        return userNotification.interactedAt != null
    }

    override suspend fun updateNotificationSent(type: NotificationType) {
        userNotificationsDao.getUserNotification(type.notificationId)
            ?.apply {
                sentThisWeek++
                lastSentAt = System.currentTimeMillis()
            }
            ?.let { userNotificationsDao.update(it) }
    }

    private suspend fun <T> setupNotificationsForType(
        values: Iterable<T>,
        getId: (T) -> Int,
    ) {
        val userNotifications = values.map { notification ->
            UserNotifications(notificationId = getId(notification))
        }
        userNotificationsDao.insert(userNotifications)
    }
}
