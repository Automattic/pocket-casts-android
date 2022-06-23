package au.com.shiftyjelly.pocketcasts.ui

import android.content.Intent
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import au.com.shiftyjelly.pocketcasts.PocketcastsApplication
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper.Companion.NOTIFICATION_ID_PLAYING
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackService
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayerNotificationManager
import au.com.shiftyjelly.pocketcasts.utils.SchedulerProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.timeout
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeoutException

@RunWith(AndroidJUnit4::class)
class PlaybackServiceTest {
    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    @Throws(TimeoutException::class)
    fun testPlaybackServiceEntersAndExitsForeground() {
        val application = ApplicationProvider.getApplicationContext<PocketcastsApplication>()
        val testScheduler = SchedulerProvider.testScheduler

        // Create the service Intent.
        val serviceIntent = Intent(
            application,
            PlaybackService::class.java
        )

        // Bind the service and grab a reference to the binder.
        val binder: IBinder = serviceRule.bindService(serviceIntent)

        // Get the reference to the service, or you can call
        // public methods on the binder directly.
        val service: PlaybackService = (binder as PlaybackService.LocalBinder).service
        val testNotificationManager = mock<PlayerNotificationManager> { }
        val testNotificationHelper = mock<NotificationHelper> {
            on { isShowing(any()) }.doReturn(true)
        }

        service.notificationManager = testNotificationManager
        service.notificationHelper = testNotificationHelper

        val metaData = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Test title")
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, UUID.randomUUID().toString())
            .build()

        val buffering = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_BUFFERING, 0, 1.0f)
            .build()
        val playing1 = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
            .build()
        val playing2 = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_PLAYING, 5, 1.0f)
            .build()

        service.testPlaybackStateChange(metaData, buffering)
        service.testPlaybackStateChange(metaData, playing1)
        service.testPlaybackStateChange(metaData, playing2)

        testScheduler.triggerActions()

        // Make sure the service enters the foreground on play
        verify(testNotificationManager, timeout(5000).atLeastOnce()).enteredForeground(any())
        assertTrue("Service should have entered the foreground", service.isForegroundService())

        clearInvocations(testNotificationManager)

        // Make sure the services exits the foreground on pause
        val paused = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_PAUSED, 6, 1.0f)
            .build()
        service.testPlaybackStateChange(metaData, paused)
        testScheduler.triggerActions()

        verify(testNotificationManager, timeout(5000).times(2)).notify(eq(NOTIFICATION_ID_PLAYING), any()) // Once for updating meta data, and then once to remove
        assertFalse("Service should have exited the foreground", service.isForegroundService())

        // Mock remove the notification so we can test we don't notify again
        val testNotificationHelperNotShowing = mock<NotificationHelper> {
            on { isShowing(any()) }.doReturn(false)
        }
        service.notificationHelper = testNotificationHelperNotShowing

        // Make sure the notification method isn't called again, this causes the notification to pop back up after being dismissed
        clearInvocations(testNotificationManager)
        val stopped = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_STOPPED, 6, 1.0f)
            .build()
        service.testPlaybackStateChange(metaData, stopped)
        testScheduler.triggerActions()
        verifyNoMoreInteractions(testNotificationManager)
    }
}
