package au.com.shiftyjelly.pocketcasts.repositories.notification

import au.com.shiftyjelly.pocketcasts.models.db.dao.NotificationsDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserNotificationsDao
import au.com.shiftyjelly.pocketcasts.models.entity.Notifications
import au.com.shiftyjelly.pocketcasts.models.entity.UserNotifications
import au.com.shiftyjelly.pocketcasts.models.type.NotificationCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private lateinit var notificationsDao: NotificationsDao
    private lateinit var userNotificationsDao: UserNotificationsDao
    private lateinit var notificationManager: NotificationManagerImpl

    @Before
    fun setUp() {
        notificationsDao = mock()
        userNotificationsDao = mock()
        notificationManager = NotificationManagerImpl(notificationsDao, userNotificationsDao)
    }

    @Test
    fun `should insert onboarding notifications`() = runTest {
        val insertedIds = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L)
        whenever(notificationsDao.insert(any())).thenReturn(insertedIds)

        notificationManager.setupOnboardingNotificationsChannels()

        val notificationsCaptor = argumentCaptor<List<Notifications>>()
        verify(notificationsDao).insert(notificationsCaptor.capture())
        val insertedNotifications = notificationsCaptor.firstValue

        val expectedNotifications = listOf(
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = OnboardingNotificationType.SUBCATEGORY_SYNC),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = OnboardingNotificationType.SUBCATEGORY_IMPORT),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = OnboardingNotificationType.SUBCATEGORY_UP_NEXT),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = OnboardingNotificationType.SUBCATEGORY_FILTERS),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = OnboardingNotificationType.SUBCATEGORY_THEMES),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = OnboardingNotificationType.SUBCATEGORY_STAFF_PICKS),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = OnboardingNotificationType.SUBCATEGORY_PLUS_UP_SELL),
        )
        assertEquals(expectedNotifications, insertedNotifications)

        val userNotificationsCaptor = argumentCaptor<List<UserNotifications>>()
        verify(userNotificationsDao).insert(userNotificationsCaptor.capture())
        val capturedUserNotifications = userNotificationsCaptor.firstValue

        val expectedUserNotifications = insertedIds.map { UserNotifications(notificationId = it.toInt()) }
        assertEquals(expectedUserNotifications, capturedUserNotifications)
    }

    @Test
    fun `should update interacted_at when tracking user interaction feature`() = runTest {
        val filterNotification = Notifications(
            category = NotificationCategory.ONBOARDING,
            subcategory = OnboardingNotificationType.SUBCATEGORY_FILTERS,
        ).apply {
            id = 4L
        }
        whenever(notificationsDao.getNotificationBySubcategory(OnboardingNotificationType.SUBCATEGORY_FILTERS))
            .thenReturn(filterNotification)

        notificationManager.trackUserInteractedWithFeature(OnboardingNotificationType.Filters)

        val idCaptor = argumentCaptor<Int>()
        val timestampCaptor = argumentCaptor<Long>()
        verify(userNotificationsDao).updateInteractedAt(idCaptor.capture(), timestampCaptor.capture())
        assertEquals(4, idCaptor.firstValue)
    }

    @Test
    fun `should return false when user has not interacted with feature`() = runTest {
        val filterNotification = Notifications(
            category = NotificationCategory.ONBOARDING,
            subcategory = OnboardingNotificationType.SUBCATEGORY_FILTERS,
        ).apply {
            id = 4L
        }
        whenever(notificationsDao.getNotificationBySubcategory(OnboardingNotificationType.SUBCATEGORY_FILTERS))
            .thenReturn(filterNotification)

        val userNotification = UserNotifications(notificationId = 4, interactedAt = null)
        whenever(userNotificationsDao.getUserNotification(4)).thenReturn(userNotification)

        val hasInteracted = notificationManager.hasUserInteractedWithFeature(OnboardingNotificationType.Filters)

        assertFalse(hasInteracted)
    }

    @Test
    fun `should return true when user has interacted with feature`() = runTest {
        val filterNotification = Notifications(
            category = NotificationCategory.ONBOARDING,
            subcategory = OnboardingNotificationType.SUBCATEGORY_FILTERS,
        ).apply {
            id = 4L
        }
        whenever(notificationsDao.getNotificationBySubcategory(OnboardingNotificationType.SUBCATEGORY_FILTERS))
            .thenReturn(filterNotification)

        val userNotification = UserNotifications(
            notificationId = 4,
            interactedAt = System.currentTimeMillis(),
        )
        whenever(userNotificationsDao.getUserNotification(4)).thenReturn(userNotification)

        val hasInteracted = notificationManager.hasUserInteractedWithFeature(OnboardingNotificationType.Filters)

        assertTrue(hasInteracted)
    }

    @Test
    fun `should update sentThisWeek and lastSentAt when tracking notification sent`() = runTest {
        val filterNotification = Notifications(
            category = NotificationCategory.ONBOARDING,
            subcategory = OnboardingNotificationType.SUBCATEGORY_FILTERS,
        ).apply {
            id = 4L
        }
        whenever(notificationsDao.getNotificationBySubcategory(OnboardingNotificationType.SUBCATEGORY_FILTERS))
            .thenReturn(filterNotification)

        val initialUserNotification = UserNotifications(
            notificationId = 4,
            sentThisWeek = 0,
            lastSentAt = 0,
        )
        whenever(userNotificationsDao.getUserNotification(4)).thenReturn(initialUserNotification)

        notificationManager.updateOnboardingNotificationSent(OnboardingNotificationType.Filters)

        val userNotificationCaptor = argumentCaptor<UserNotifications>()
        verify(userNotificationsDao).update(userNotificationCaptor.capture())
        val updatedUserNotification = userNotificationCaptor.firstValue

        assertEquals(1, updatedUserNotification.sentThisWeek)
        assertTrue(updatedUserNotification.lastSentAt > 0)
    }

    @Test
    fun `should not update notification sent when notification is null`() = runTest {
        whenever(notificationsDao.getNotificationBySubcategory(OnboardingNotificationType.SUBCATEGORY_FILTERS))
            .thenReturn(null)

        notificationManager.updateOnboardingNotificationSent(OnboardingNotificationType.Filters)

        verify(userNotificationsDao, never()).update(any())
    }

    @Test
    fun `should not update notification sent when user notification is null`() = runTest {
        val filterNotification = Notifications(
            category = NotificationCategory.ONBOARDING,
            subcategory = OnboardingNotificationType.SUBCATEGORY_FILTERS,
        ).apply {
            id = 4L
        }
        whenever(notificationsDao.getNotificationBySubcategory(OnboardingNotificationType.SUBCATEGORY_FILTERS))
            .thenReturn(filterNotification)

        whenever(userNotificationsDao.getUserNotification(4)).thenReturn(null)

        notificationManager.updateOnboardingNotificationSent(OnboardingNotificationType.Filters)

        verify(userNotificationsDao, never()).update(any())
    }
}
