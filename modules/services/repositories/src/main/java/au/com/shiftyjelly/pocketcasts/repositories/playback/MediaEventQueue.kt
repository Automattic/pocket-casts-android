package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.os.SystemClock
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class MediaEventQueue(
    private val scopeProvider: () -> CoroutineScope,
    // Deadlines are measured against elapsed realtime so the window still closes while the device is suspended.
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) {
    private var singleTapJob: SingleTapJob? = null
    private var multiTapWindowEndsAt = Long.MIN_VALUE
    private val stateMutex = Mutex()

    private val scope: CoroutineScope get() = scopeProvider()

    private val isMultiTapWindowOpen get() = elapsedRealtime() <= multiTapWindowEndsAt

    /**
     * [onImmediateSingleTap] returns whether it handled the tap. A tap it declines still resolves through the
     * window so the caller keeps its normal single tap action.
     */
    suspend fun consumeEvent(
        event: MediaEvent,
        onImmediateSingleTap: (() -> Boolean)? = null,
    ) = when (event) {
        MediaEvent.SingleTap -> handleSingleTapEvent(onImmediateSingleTap)
        MediaEvent.DoubleTap, MediaEvent.TripleTap -> handleMultiTapEvent(event)
    }

    private suspend fun handleSingleTapEvent(onImmediateSingleTap: (() -> Boolean)?): MediaEvent? {
        val newSingleTapJob = stateMutex.withLock {
            val currentSingleTapJob = singleTapJob
            when {
                // Pixel Buds (and possibly other headphones) trigger KEYCODE_MEDIA_PLAY
                // after KEYCODE_MEDIA_NEXT or KEYCODE_MEDIA_PREVIOUS.
                // We need to ignore it so the single tap action isn't triggered in such cases.
                isMultiTapWindowOpen -> null

                currentSingleTapJob?.isActive == true -> {
                    currentSingleTapJob.incrementTaps()
                    null
                }

                else -> SingleTapJob(scope, elapsedRealtime).also { singleTapJob = it }
            }
        } ?: return null

        val immediateTapHandled = try {
            onImmediateSingleTap?.invoke() == true
        } catch (e: Exception) {
            stateMutex.withLock {
                if (singleTapJob === newSingleTapJob) {
                    singleTapJob = null
                    newSingleTapJob.cancel()
                }
            }
            throw e
        }
        newSingleTapJob.await()
        return stateMutex.withLock {
            // A handled immediate callback owns a resolved SingleTap. Follow-up taps still
            // return their DoubleTap or TripleTap action after the window closes.
            newSingleTapJob.event().takeUnless {
                it == MediaEvent.SingleTap && immediateTapHandled
            }
        }
    }

    private suspend fun handleMultiTapEvent(event: MediaEvent): MediaEvent = stateMutex.withLock {
        multiTapWindowEndsAt = elapsedRealtime() + MULTI_TAP_WINDOW_MS
        event
    }

    private class SingleTapJob(
        scope: CoroutineScope,
        private val elapsedRealtime: () -> Long,
    ) {
        private var counter: Int = 1

        private val startedAt = elapsedRealtime()

        private val job = scope.launch { delay(SINGLE_TAP_WINDOW_MS) }

        val isActive get() = job.isActive

        suspend fun await() {
            job.join()
            // The window runs on a timer that stops while the device is suspended, so it can outlast its budget.
            val waited = elapsedRealtime() - startedAt
            if (waited > SINGLE_TAP_WINDOW_MS * 2) {
                LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Media button tap window took ${waited}ms for a ${SINGLE_TAP_WINDOW_MS}ms budget")
            }
        }

        fun cancel() = job.cancel()

        fun incrementTaps() {
            counter++
        }

        fun event() = when (counter) {
            1 -> MediaEvent.SingleTap
            2 -> MediaEvent.DoubleTap
            else -> MediaEvent.TripleTap
        }
    }

    private companion object {
        const val MULTI_TAP_WINDOW_MS = 250L
        const val SINGLE_TAP_WINDOW_MS = 600L
    }
}

internal enum class MediaEvent {
    SingleTap,
    DoubleTap,
    TripleTap,
}
