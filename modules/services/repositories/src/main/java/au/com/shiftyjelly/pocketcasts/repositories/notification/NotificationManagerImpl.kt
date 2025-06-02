package au.com.shiftyjelly.pocketcasts.repositories.notification

import au.com.shiftyjelly.pocketcasts.models.db.dao.UserNotificationsDao
import au.com.shiftyjelly.pocketcasts.models.entity.UserNotifications
import java.time.Clock
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class NotificationManagerImpl @Inject constructor(
    private val userNotificationsDao: UserNotificationsDao,
    private val clock: Clock,
) : NotificationManager {

    override suspend fun setupOnboardingNotifications() {
        setupNotificationsForType(OnboardingNotificationType.values) { it.notificationId }
    }

    override suspend fun setupReEngagementNotifications() {
        setupNotificationsForType(ReEngagementNotificationType.values) { it.notificationId }
    }

    override suspend fun setupTrendingAndRecommendationsNotifications() {
        setupNotificationsForType(TrendingAndRecommendationsNotificationType.values) { it.notificationId }
    }

    override suspend fun setupNewFeaturesNotifications() {
        setupNotificationsForType(NewFeaturesAndTipsNotificationType.values) { it.notificationId }
    }

    override suspend fun setupOffersNotifications() {
        setupNotificationsForType(OffersNotificationType.values) { it.notificationId }
    }

    override suspend fun updateUserFeatureInteraction(type: NotificationType) {
        val now = clock.instant().toEpochMilli()
        userNotificationsDao.updateInteractedAt(type.notificationId, now)
    }

    override suspend fun updateUserFeatureInteraction(id: Int) {
        val now = clock.instant().toEpochMilli()
        userNotificationsDao.updateInteractedAt(id, now)
    }

    override suspend fun hasUserInteractedWithFeature(type: NotificationType): Boolean {
        val userNotification = userNotificationsDao.getUserNotification(type.notificationId)
            ?: return false

        if (type is ReEngagementNotificationType) {
            val lastInteraction = userNotification.interactedAt ?: return false
            val elapsed = (clock.instant().toEpochMilli() - lastInteraction).milliseconds
            return elapsed < 7.days
        }

        return userNotification.interactedAt != null
    }

    override suspend fun updateNotificationSent(type: NotificationType) {
        val now = clock.instant().toEpochMilli()
        userNotificationsDao.getUserNotification(type.notificationId)
            ?.apply {
                sentThisWeek++
                lastSentAt = now
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
