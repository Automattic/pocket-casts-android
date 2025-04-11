package au.com.shiftyjelly.pocketcasts.repositories.notification

import au.com.shiftyjelly.pocketcasts.models.db.dao.NotificationsDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserNotificationsDao
import au.com.shiftyjelly.pocketcasts.models.entity.Notifications
import au.com.shiftyjelly.pocketcasts.models.entity.UserNotifications
import au.com.shiftyjelly.pocketcasts.models.type.NotificationCategory
import javax.inject.Inject

class NotificationManagerImpl @Inject constructor(
    private val notificationsDao: NotificationsDao,
    private val userNotificationsDao: UserNotificationsDao,
) : NotificationManager {

    override suspend fun setupOnboardingNotificationsChannels() {
        val notifications = listOf(
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = OnboardingNotificationType.SUBCATEGORY_SYNC),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = OnboardingNotificationType.SUBCATEGORY_IMPORT),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = OnboardingNotificationType.SUBCATEGORY_UP_NEXT),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = OnboardingNotificationType.SUBCATEGORY_FILTERS),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = OnboardingNotificationType.SUBCATEGORY_THEMES),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = OnboardingNotificationType.SUBCATEGORY_STAFF_PICKS),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = OnboardingNotificationType.SUBCATEGORY_PLUS_UP_SELL),
        )

        val insertedIds: List<Long> = notificationsDao.insert(notifications)

        val userNotifications = insertedIds.map { id ->
            UserNotifications(notificationId = id.toInt())
        }

        userNotificationsDao.insert(userNotifications)
    }

    override suspend fun trackUserInteractedWithFeature(type: OnboardingNotificationType) {
        val filterNotification = notificationsDao.getNotificationBySubcategory(type.subcategory)

        val notificationId = filterNotification?.id?.toInt() ?: return
        userNotificationsDao.updateInteractedAt(notificationId, System.currentTimeMillis())
    }

    override suspend fun hasUserInteractedWithFeature(type: OnboardingNotificationType): Boolean {
        val filtersNotification = notificationsDao.getNotificationBySubcategory(type.subcategory)
            ?: return false

        val notificationId = filtersNotification.id?.toInt()
            ?: return false

        val userNotification = userNotificationsDao.getUserNotification(notificationId)
            ?: return false

        return userNotification.interactedAt != null
    }

    override suspend fun updateOnboardingNotificationSent(type: OnboardingNotificationType) {
        val filterNotification = notificationsDao.getNotificationBySubcategory(type.subcategory)

        val notificationId = filterNotification?.id?.toInt()
            ?: return

        userNotificationsDao.getUserNotification(notificationId)
            ?.apply {
                sentThisWeek++
                lastSentAt = System.currentTimeMillis()
            }
            ?.let { userNotificationsDao.update(it) }
    }
}
