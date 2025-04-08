package au.com.shiftyjelly.pocketcasts.repositories.notification

import au.com.shiftyjelly.pocketcasts.models.db.dao.NotificationsDao
import au.com.shiftyjelly.pocketcasts.models.entity.Notifications
import au.com.shiftyjelly.pocketcasts.models.type.NotificationCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class NotificationManagerTest {

    private lateinit var notificationsDao: NotificationsDao
    private lateinit var notificationManager: NotificationManagerImpl

    @Before
    fun setUp() {
        notificationsDao = mock()
        notificationManager = NotificationManagerImpl(notificationsDao)
    }

    @Test
    fun `should insert onboarding notifications`() = runTest {
        notificationManager.setupOnboardingNotifications()

        val notificationsCaptor = argumentCaptor<List<Notifications>>()
        verify(notificationsDao).insert(notificationsCaptor.capture())

        val insertedNotifications = notificationsCaptor.firstValue

        val expectedNotifications = listOf(
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = NotificationManagerImpl.SUBCATEGORY_SYNC),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = NotificationManagerImpl.SUBCATEGORY_IMPORT),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = NotificationManagerImpl.SUBCATEGORY_UP_NEXT),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = NotificationManagerImpl.SUBCATEGORY_FILTERS),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = NotificationManagerImpl.SUBCATEGORY_THEMES),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = NotificationManagerImpl.SUBCATEGORY_STAFF_PICKS),
            Notifications(category = NotificationCategory.ONBOARDING, subcategory = NotificationManagerImpl.SUBCATEGORY_PLUS_UP_SELL),
        )

        assertEquals(expectedNotifications, insertedNotifications)
    }
}
