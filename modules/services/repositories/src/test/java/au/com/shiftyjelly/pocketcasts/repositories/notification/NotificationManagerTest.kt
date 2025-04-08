package au.com.shiftyjelly.pocketcasts.repositories.notification

import au.com.shiftyjelly.pocketcasts.models.db.dao.NotificationsDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserNotificationsDao
import au.com.shiftyjelly.pocketcasts.models.entity.Notifications
import au.com.shiftyjelly.pocketcasts.models.entity.UserNotifications
import au.com.shiftyjelly.pocketcasts.models.type.NotificationCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
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

        notificationManager.setupOnboardingNotifications()

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
}
