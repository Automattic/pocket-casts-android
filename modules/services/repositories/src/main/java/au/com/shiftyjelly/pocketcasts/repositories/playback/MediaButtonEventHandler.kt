package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.view.KeyEvent
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Serializes media-button key events before dispatching their resolved actions.
 *
 * Event registration starts synchronously to preserve framework callback order. [onImmediatePlay] may therefore run
 * on the caller's stack and must stay fast. [onMediaEvent] runs only after a suspension boundary, outside that
 * synchronous registration section.
 */
internal class MediaButtonEventHandler(
    private val scopeProvider: () -> CoroutineScope,
    private val onImmediatePlay: () -> Unit,
    private val onMediaEvent: (MediaEvent) -> Unit,
    private val onError: (Exception) -> Unit = {
        LogBuffer.e(LogBuffer.TAG_PLAYBACK, it, "Media button event handling failed")
    },
) {
    private val mediaEventQueue = MediaEventQueue(scopeProvider)

    private val scope: CoroutineScope get() = scopeProvider()

    fun handle(keyEvent: KeyEvent): Boolean {
        if (keyEvent.action != KeyEvent.ACTION_DOWN) {
            return false
        }

        val inputEvent = when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK,
            -> MediaEvent.SingleTap

            KeyEvent.KEYCODE_MEDIA_NEXT -> MediaEvent.DoubleTap

            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MediaEvent.TripleTap

            else -> null
        } ?: return false

        val immediateSingleTapHandler = if (keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            onImmediatePlay
        } else {
            null
        }

        // Register the event before returning to the framework callback. This preserves
        // delivery order while the queue's timeout still resumes on the provided scope.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                coroutineContext.ensureActive()
                val outputEvent = mediaEventQueue.consumeEvent(
                    event = inputEvent,
                    onImmediateSingleTap = immediateSingleTapHandler,
                )
                if (outputEvent != null) {
                    // Output actions historically ran asynchronously on the callback scope.
                    yield()
                    onMediaEvent(outputEvent)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e)
            }
        }
        return true
    }
}
