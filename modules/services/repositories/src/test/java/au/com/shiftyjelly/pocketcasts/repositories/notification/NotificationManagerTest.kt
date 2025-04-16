package au.com.shiftyjelly.pocketcasts.repositories.notification

import au.com.shiftyjelly.pocketcasts.models.db.dao.UserNotificationsDao
import au.com.shiftyjelly.pocketcasts.models.entity.UserNotifications
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType.Filters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NotificationManagerTest {

    private lateinit var userNotificationsDao: UserNotificationsDao
    private lateinit var notificationManager: NotificationManagerImpl

    @Before
    fun setUp() {
        userNotificationsDao = mock()
        notificationManager = NotificationManagerImpl(userNotificationsDao)
    }

    @Test
    fun `should setup onboarding notifications`() = runTest {
        val insertedIds = OnboardingNotificationType.values.map { it.notificationId }

        notificationManager.setupOnboardingNotifications()

        val userNotificationsCaptor = argumentCaptor<List<UserNotifications>>()
        verify(userNotificationsDao).insert(userNotificationsCaptor.capture())
        val capturedUserNotifications = userNotificationsCaptor.firstValue

        val expectedUserNotifications = insertedIds.map { UserNotifications(notificationId = it.toInt()) }
        assertEquals(expectedUserNotifications, capturedUserNotifications)
    }

    @Test
    fun `should setup re-engagement notifications`() = runTest {
        val insertedIds = ReEngagementNotificationType.values.map { it.notificationId }

        notificationManager.setupReEngagementNotifications()

        val userNotificationsCaptor = argumentCaptor<List<UserNotifications>>()
        verify(userNotificationsDao).insert(userNotificationsCaptor.capture())
        val capturedUserNotifications = userNotificationsCaptor.firstValue

        val expectedUserNotifications = insertedIds.map { UserNotifications(notificationId = it.toInt()) }
        assertEquals(expectedUserNotifications, capturedUserNotifications)
    }

    @Test
    fun `should update interacted_at when tracking user interaction feature`() = runTest {
        notificationManager.updateUserFeatureInteraction(Filters)

        val idCaptor = argumentCaptor<Int>()
        val timestampCaptor = argumentCaptor<Long>()
        verify(userNotificationsDao).updateInteractedAt(idCaptor.capture(), timestampCaptor.capture())
        assertNotEquals(0, idCaptor.firstValue)
    }

    @Test
    fun `should update interacted_at when tracking user interaction feature passing id`() = runTest {
        val id = 99

        notificationManager.updateUserFeatureInteraction(id)

        val idCaptor = argumentCaptor<Int>()
        val timestampCaptor = argumentCaptor<Long>()
        verify(userNotificationsDao).updateInteractedAt(idCaptor.capture(), timestampCaptor.capture())
        assertEquals(id, idCaptor.firstValue)
    }

    @Test
    fun `should return false when user has not interacted with feature`() = runTest {
        val userNotification = UserNotifications(notificationId = Filters.notificationId, interactedAt = null)
        whenever(userNotificationsDao.getUserNotification(Filters.notificationId)).thenReturn(userNotification)

        val hasInteracted = notificationManager.hasUserInteractedWithFeature(Filters)

        assertFalse(hasInteracted)
    }

    @Test
    fun `should return true when user has interacted with feature`() = runTest {
        val userNotification = UserNotifications(
            notificationId = Filters.notificationId,
            interactedAt = System.currentTimeMillis(),
        )
        whenever(userNotificationsDao.getUserNotification(Filters.notificationId)).thenReturn(userNotification)

        val hasInteracted = notificationManager.hasUserInteractedWithFeature(Filters)

        assertTrue(hasInteracted)
    }

    @Test
    fun `should return false when user interacted exactly 7 days ago`() = runTest {
        val now = System.currentTimeMillis()
        val sevenDaysInMillis = TimeUnit.DAYS.toMillis(7)

        val userNotification = UserNotifications(
            notificationId = ReEngagementNotificationType.WeMissYou.notificationId,
            interactedAt = now - sevenDaysInMillis,
        )
        whenever(userNotificationsDao.getUserNotification(ReEngagementNotificationType.WeMissYou.notificationId))
            .thenReturn(userNotification)

        val hasInteracted = notificationManager.hasUserInteractedWithFeature(ReEngagementNotificationType.WeMissYou)

        assertFalse(hasInteracted)
    }

    @Test
    fun `should return true when user interacted just less than 7 days ago`() = runTest {
        val now = System.currentTimeMillis()
        val sevenDaysInMillis = TimeUnit.DAYS.toMillis(7)

        val userNotification = UserNotifications(
            notificationId = ReEngagementNotificationType.WeMissYou.notificationId,
            interactedAt = now - sevenDaysInMillis + 1,
        )
        whenever(userNotificationsDao.getUserNotification(ReEngagementNotificationType.WeMissYou.notificationId))
            .thenReturn(userNotification)

        val hasInteracted = notificationManager.hasUserInteractedWithFeature(ReEngagementNotificationType.WeMissYou)

        assertTrue(hasInteracted)
    }

    @Test
    fun `should return false when user interacted more than 7 days ago`() = runTest {
        val now = System.currentTimeMillis()
        val sevenDaysInMillis = TimeUnit.DAYS.toMillis(7)

        val userNotification = UserNotifications(
            notificationId = ReEngagementNotificationType.WeMissYou.notificationId,
            interactedAt = now - sevenDaysInMillis - 1,
        )
        whenever(userNotificationsDao.getUserNotification(ReEngagementNotificationType.WeMissYou.notificationId))
            .thenReturn(userNotification)

        val hasInteracted = notificationManager.hasUserInteractedWithFeature(ReEngagementNotificationType.WeMissYou)

        assertFalse(hasInteracted)
    }

    @Test
    fun `should update sentThisWeek and lastSentAt when tracking notification sent`() = runTest {
        val initialUserNotification = UserNotifications(
            notificationId = Filters.notificationId,
            sentThisWeek = 0,
            lastSentAt = 0,
        )
        whenever(userNotificationsDao.getUserNotification(Filters.notificationId)).thenReturn(initialUserNotification)

        notificationManager.updateNotificationSent(Filters)

        val userNotificationCaptor = argumentCaptor<UserNotifications>()
        verify(userNotificationsDao).update(userNotificationCaptor.capture())
        val updatedUserNotification = userNotificationCaptor.firstValue

        assertEquals(1, updatedUserNotification.sentThisWeek)
        assertTrue(updatedUserNotification.lastSentAt > 0)
    }

    @Test
    fun `should not update notification sent when notification is null`() = runTest {
        notificationManager.updateNotificationSent(Filters)

        verify(userNotificationsDao, never()).update(any())
    }

    @Test
    fun `should not update notification sent when user notification is null`() = runTest {
        whenever(userNotificationsDao.getUserNotification(4)).thenReturn(null)

        notificationManager.updateNotificationSent(Filters)

        verify(userNotificationsDao, never()).update(any())
    }
}
