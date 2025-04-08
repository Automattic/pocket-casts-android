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

    override suspend fun setupOnboardingNotifications() {
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
}
