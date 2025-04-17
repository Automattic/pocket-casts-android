package au.com.shiftyjelly.pocketcasts.repositories.notification

import au.com.shiftyjelly.pocketcasts.models.db.dao.UserNotificationsDao
import au.com.shiftyjelly.pocketcasts.models.entity.UserNotifications
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType.Filters
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NotificationManagerTest {

    companion object {
        private fun fixedManager(
            dao: UserNotificationsDao,
            fixedTime: Long,
        ): NotificationManagerImpl {
            val clock = Clock.fixed(Instant.ofEpochMilli(fixedTime), ZoneOffset.UTC)
            return NotificationManagerImpl(dao, clock)
        }
    }

    @Test fun `should setup onboarding notifications`() = runTest {
        val dao = mock<UserNotificationsDao>()
        val now = Instant.parse("2025-04-17T00:00:00Z").toEpochMilli()
        val manager = fixedManager(dao, now)

        val insertedIds = OnboardingNotificationType.values.map { it.notificationId }
        manager.setupOnboardingNotifications()

        val captor = argumentCaptor<List<UserNotifications>>()
        verify(dao).insert(captor.capture())
        val actual = captor.firstValue
        val expected = insertedIds.map { UserNotifications(notificationId = it.toInt()) }
        assertEquals(expected, actual)
    }

    @Test fun `should setup re-engagement notifications`() = runTest {
        val dao = mock<UserNotificationsDao>()
        val now = Instant.parse("2025-04-17T00:00:00Z").toEpochMilli()
        val manager = fixedManager(dao, now)

        val insertedIds = ReEngagementNotificationType.values.map { it.notificationId }
        manager.setupReEngagementNotifications()

        val captor = argumentCaptor<List<UserNotifications>>()
        verify(dao).insert(captor.capture())
        val actual = captor.firstValue
        val expected = insertedIds.map { UserNotifications(notificationId = it.toInt()) }
        assertEquals(expected, actual)
    }

    @Test fun `should update interacted_at when tracking user interaction feature`() = runTest {
        val dao = mock<UserNotificationsDao>()
        val now = 1_625_000_000_000L
        val manager = fixedManager(dao, now)

        manager.updateUserFeatureInteraction(Filters)

        val idCap = argumentCaptor<Int>()
        val timeCap = argumentCaptor<Long>()
        verify(dao).updateInteractedAt(idCap.capture(), timeCap.capture())
        assertEquals(Filters.notificationId, idCap.firstValue)
        assertEquals(now, timeCap.firstValue)
    }

    @Test fun `should update interacted_at when tracking user interaction feature passing id`() = runTest {
        val dao = mock<UserNotificationsDao>()
        val now = 1_625_000_000_000L
        val manager = fixedManager(dao, now)
        val id = 99

        manager.updateUserFeatureInteraction(id)

        val idCap = argumentCaptor<Int>()
        val timeCap = argumentCaptor<Long>()
        verify(dao).updateInteractedAt(idCap.capture(), timeCap.capture())
        assertEquals(id, idCap.firstValue)
        assertEquals(now, timeCap.firstValue)
    }

    @Test fun `should return false when user has not interacted with feature`() = runTest {
        val dao = mock<UserNotificationsDao>()
        val now = 1_625_000_000_000L
        val manager = fixedManager(dao, now)
        whenever(dao.getUserNotification(Filters.notificationId))
            .thenReturn(UserNotifications(Filters.notificationId, interactedAt = null))

        assertFalse(manager.hasUserInteractedWithFeature(Filters))
    }

    @Test fun `should return true when user has interacted with feature`() = runTest {
        val dao = mock<UserNotificationsDao>()
        val now = 1_625_000_000_000L
        val manager = fixedManager(dao, now)
        whenever(dao.getUserNotification(Filters.notificationId))
            .thenReturn(UserNotifications(Filters.notificationId, interactedAt = now))

        assertTrue(manager.hasUserInteractedWithFeature(Filters))
    }

    @Test fun `should return false when user interacted exactly 7 days ago`() = runTest {
        val dao = mock<UserNotificationsDao>()
        val now = 1_625_000_000_000L
        val sevenDays = TimeUnit.DAYS.toMillis(7)
        val manager = fixedManager(dao, now)
        whenever(dao.getUserNotification(ReEngagementNotificationType.WeMissYou.notificationId))
            .thenReturn(UserNotifications(ReEngagementNotificationType.WeMissYou.notificationId, interactedAt = now - sevenDays))

        assertFalse(manager.hasUserInteractedWithFeature(ReEngagementNotificationType.WeMissYou))
    }

    @Test fun `should return true when user interacted just less than 7 days ago`() = runTest {
        val dao = mock<UserNotificationsDao>()
        val now = 1_625_000_000_000L
        val sevenDays = TimeUnit.DAYS.toMillis(7)
        val manager = fixedManager(dao, now)
        whenever(dao.getUserNotification(ReEngagementNotificationType.WeMissYou.notificationId))
            .thenReturn(UserNotifications(ReEngagementNotificationType.WeMissYou.notificationId, interactedAt = now - sevenDays + 1))

        assertTrue(manager.hasUserInteractedWithFeature(ReEngagementNotificationType.WeMissYou))
    }

    @Test fun `should return false when user interacted more than 7 days ago`() = runTest {
        val dao = mock<UserNotificationsDao>()
        val now = 1_625_000_000_000L
        val sevenDays = TimeUnit.DAYS.toMillis(7)
        val manager = fixedManager(dao, now)
        whenever(dao.getUserNotification(ReEngagementNotificationType.WeMissYou.notificationId))
            .thenReturn(UserNotifications(ReEngagementNotificationType.WeMissYou.notificationId, interactedAt = now - sevenDays - 1))

        assertFalse(manager.hasUserInteractedWithFeature(ReEngagementNotificationType.WeMissYou))
    }

    @Test fun `should update sentThisWeek and lastSentAt when tracking notification sent`() = runTest {
        val dao = mock<UserNotificationsDao>()
        val now = 1_625_000_000_000L
        val manager = fixedManager(dao, now)

        whenever(dao.getUserNotification(Filters.notificationId))
            .thenReturn(UserNotifications(Filters.notificationId, sentThisWeek = 0, lastSentAt = 0))

        manager.updateNotificationSent(Filters)

        val captor = argumentCaptor<UserNotifications>()
        verify(dao).update(captor.capture())
        val updated = captor.firstValue
        assertEquals(1, updated.sentThisWeek)
        assertEquals(now, updated.lastSentAt)
    }

    @Test fun `should not update notification sent when notification is null`() = runTest {
        val dao = mock<UserNotificationsDao>()
        val now = 1_625_000_000_000L
        val manager = fixedManager(dao, now)

        manager.updateNotificationSent(Filters)
        verify(dao, never()).update(any())
    }

    @Test fun `should not update notification sent when user notification is null`() = runTest {
        val dao = mock<UserNotificationsDao>()
        val now = 1_625_000_000_000L
        val manager = fixedManager(dao, now)

        whenever(dao.getUserNotification(4)).thenReturn(null)
        manager.updateNotificationSent(Filters)
        verify(dao, never()).update(any())
    }
}
