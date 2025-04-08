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

    companion object {
        const val SUBCATEGORY_SYNC = "sync"
        const val SUBCATEGORY_IMPORT = "import"
        const val SUBCATEGORY_UP_NEXT = "up_next"
        const val SUBCATEGORY_FILTERS = "filters"
        const val SUBCATEGORY_THEMES = "themes"
        const val SUBCATEGORY_STAFF_PICKS = "staff_picks"
        const val SUBCATEGORY_PLUS_UP_SELL = "plus_upsell"
    }

    override suspend fun setupOnboardingNotifications() {
        val notifications = listOf(
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = SUBCATEGORY_SYNC),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = SUBCATEGORY_IMPORT),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = SUBCATEGORY_UP_NEXT),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = SUBCATEGORY_FILTERS),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = SUBCATEGORY_THEMES),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = SUBCATEGORY_STAFF_PICKS),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = SUBCATEGORY_PLUS_UP_SELL),
        )

        val insertedIds: List<Long> = notificationsDao.insert(notifications)

        val userNotifications = insertedIds.map { id ->
            UserNotifications(notificationId = id.toInt())
        }

        userNotificationsDao.insert(userNotifications)
    }
}
