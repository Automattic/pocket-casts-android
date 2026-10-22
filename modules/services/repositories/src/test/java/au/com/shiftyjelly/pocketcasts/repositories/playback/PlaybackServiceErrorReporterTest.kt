package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PlaybackForegroundServiceErrorEvent
import com.automattic.eventhorizon.PlaybackServiceStartErrorEvent
import com.automattic.eventhorizon.PlaybackServiceType
import com.automattic.eventhorizon.SourceViewType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [31])
@RunWith(RobolectricTestRunner::class)
class PlaybackServiceErrorReporterTest {
    private val eventSink = TestEventSink()
    private val lifecycleOwner = FakeLifecycleOwner().apply { registry.currentState = Lifecycle.State.CREATED }

    private val reporter = PlaybackServiceErrorReporter(
        eventHorizon = EventHorizon(eventSink),
        processLifecycleOwner = lifecycleOwner,
    )

    @Test
    fun `service start failure is reported`() {
        reporter.trackServiceStartFailed(
            service = PlaybackServiceType.Media3,
            error = IllegalStateException("app is in background"),
            source = SourceView.WIDGET_PLAYER_SMALL,
        )

        assertEquals(
            PlaybackServiceStartErrorEvent(
                service = PlaybackServiceType.Media3,
                appInBackground = true,
                exception = "java.lang.IllegalStateException",
                source = SourceViewType.WidgetPlayerSmall,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `service start failure before playback has a source is reported without one`() {
        reporter.trackServiceStartFailed(
            service = PlaybackServiceType.Legacy,
            error = IllegalStateException("app is in background"),
            source = null,
        )

        assertEquals(null, (eventSink.pollEvent() as PlaybackServiceStartErrorEvent).source)
    }

    @Test
    fun `foreground start failure is reported`() {
        reporter.trackForegroundStartFailed(
            service = PlaybackServiceType.Legacy,
            error = IllegalStateException("not allowed"),
            source = SourceView.WIDGET_PLAYER_SMALL,
            playbackContinued = true,
        )

        assertEquals(
            PlaybackForegroundServiceErrorEvent(
                service = PlaybackServiceType.Legacy,
                appInBackground = true,
                playbackContinued = true,
                occurrence = 1,
                exception = "java.lang.IllegalStateException",
                source = SourceViewType.WidgetPlayerSmall,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `repeated foreground failures are counted within a playback session`() {
        repeat(3) { reporter.trackForegroundStartFailed() }

        assertEquals(listOf(1L, 2L, 3L), eventSink.occurrences())
    }

    @Test
    fun `resetting the failure count starts a new playback session`() {
        reporter.trackForegroundStartFailed()
        reporter.trackForegroundStartFailed()
        reporter.resetFailureCount()
        reporter.trackForegroundStartFailed()

        assertEquals(listOf(1L, 2L, 1L), eventSink.occurrences())
    }

    @Test
    fun `an app in the foreground is reported as such`() {
        lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED

        reporter.trackForegroundStartFailed()

        assertEquals(false, eventSink.pollForegroundError().appInBackground)
    }

    @Test
    fun `an app that has been backgrounded is reported as such`() {
        lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED
        lifecycleOwner.registry.currentState = Lifecycle.State.CREATED

        reporter.trackForegroundStartFailed()

        assertEquals(true, eventSink.pollForegroundError().appInBackground)
    }

    private fun PlaybackServiceErrorReporter.trackForegroundStartFailed(
        error: Throwable = IllegalStateException(),
    ) = trackForegroundStartFailed(
        service = PlaybackServiceType.Media3,
        error = error,
        source = null,
        playbackContinued = true,
    )

    private fun TestEventSink.pollForegroundError() = pollEvent() as PlaybackForegroundServiceErrorEvent

    private fun TestEventSink.occurrences() = List(size) { pollForegroundError().occurrence }

    private class FakeLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }
}
